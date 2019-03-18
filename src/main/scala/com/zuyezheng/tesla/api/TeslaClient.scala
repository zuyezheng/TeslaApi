package com.zuyezheng.tesla.api

import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl.{Framing, Keep, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.zuyezheng.lang.TryWith
import com.zuyezheng.tesla.api.TeslaClient.ClientCreds
import play.api.libs.json.Reads._
import play.api.libs.json.{Reads, _}
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.{StandaloneWSClient, WSAuthScheme}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.{Source => IoSource}

/**
  * Client to interact with the tesla API.
  *
  * @author zuye.zheng
  */
class TeslaClient(val wsClient: StandaloneWSClient, val email: String, val clientCreds: ClientCreds, val authResponse: AuthResponse) {

    def vehicles: Future[List[Vehicle]] = {
        this.wsClient.url(TeslaClient.BaseUrl + "api/1/vehicles")
            .addHttpHeaders(("Authorization", "Bearer " + this.authResponse.accessToken))
            .get()
            .map(response => {
                (Json.parse(response.body) \ "response").as[List[Vehicle]]
            })
    }
    
    /**
      * Deprecated long polling streaming endpoint.
      */
    def streamLegacy(vehicle: Vehicle): Future[Source[String, _]] = {
        this.wsClient.url("https://streaming.vn.teslamotors.com/stream/"
            + vehicle.vehicleId
            + "?values="
            + StreamingProperty.keys.mkString(",")
        )
            .withAuth(this.email, vehicle.tokens.head, WSAuthScheme.BASIC)
            .withRequestTimeout(10 minutes)
            .stream().map(response => response.status match {
                case 200 =>
                    response.bodyAsSource
                        .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024))
                        .map(_.utf8String.trim)
                case _ =>
                    Source.failed(new Exception("Could not stream: (" + response.status + ") " + response.statusText))
            })
    }
    
    /**
      * Websocket streaming endpoint.
      */
    def stream(vehicle: Vehicle)(implicit system: ActorSystem, materializer: ActorMaterializer): Source[StreamingFrame, NotUsed] = {
        val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest("wss://streaming.vn.teslamotors.com/streaming/"))

        val messageSource: Source[Message, ActorRef] = Source.actorRef[TextMessage.Strict](
            bufferSize = 10, OverflowStrategy.fail
        )

        val ((socket, upgradeResponse), stream) = messageSource.viaMat(webSocketFlow)(Keep.both)
            // convert the message into a string which we will parse into json, using collect since an incomplete match
            // but we don't expect anything else
            .collect({
                case TextMessage.Strict(data) => data
                case BinaryMessage.Strict(data) => data.utf8String
            })
            .map(s => {
                println(s)
        
                // parse the string into a streaming frame
                val json = Json.parse(s)
                (json \ "msg_type").as[String].split(":").head match {
                    case "control" => json.as[StreamingFrame.Control]
                    case "data" => json.as[StreamingFrame.Data]
                    case _ => json.as[StreamingFrame.Unknown]
                }
            })
            .map {
                case frame@StreamingFrame.Data(_, _, _, Some(e)) => e match {
                    // not really an error, wait for vehicle to connect again
                    case "vehicle_disconnected" => frame
                    // something probably went wrong, probably want to re-authenticate
                    case _ => throw new RuntimeException(e)
                }
                case frame => frame
            }
            .preMaterialize()

        val connected = upgradeResponse.flatMap { upgrade =>
            if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
                // authenticate and request stream once connected
                socket ! TextMessage.Strict(Json.stringify(Json.obj(
                    "msg_type" -> "data:subscribe",
                    "tag" -> vehicle.vehicleId.toString,
                    "value" -> StreamingProperty.keys.mkString(","),
                    "token" -> Base64.getEncoder.encodeToString(
                        s"${this.email}:${vehicle.tokens.head}".getBytes(StandardCharsets.UTF_8)
                    )
                )))

                Future.successful(Done)
            } else {
                throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
            }
        }

        connected.onComplete(_ => println("Connected to streaming API."))

        stream
    }

    /**
      * Return a new client authenticated with the refresh token.
      */
    def refresh(): Future[TeslaClient] = {
        this.wsClient.url(TeslaClient.AuthUrl)
            .post(Map(
                "client_id" -> this.clientCreds.clientId,
                "client_secret" -> this.clientCreds.clientSecret,
                "grant_type" -> "refresh_token",
                "refresh_token" -> this.authResponse.refreshToken
            ))
            .map(response => new TeslaClient(
                wsClient, this.email, this.clientCreds, Json.parse(response.body).as[AuthResponse]
            ))
    }
    
    /**
      * Try to get a new authenticated client and vehicle with the same VIN. Vehicles will have new tokens for streaming
      * so they will need to be fetched again.
      */
    def refresh(vehicle: Vehicle): Future[(TeslaClient, Option[Vehicle])] = {
        this.refresh().flatMap(client => client.vehicles
            .map(_.find(_.vin == vehicle.vin))
            .map((client, _))
        )
    }

}

object TeslaClient {

    private val BaseUrl = "https://owner-api.teslamotors.com/"
    private val AuthUrl = BaseUrl + "oauth/token"

    def apply(wsClient: StandaloneWSClient, clientSpec: URL, email: String, password: String): Future[TeslaClient] = {
        TryWith().execute(resources => {
            val credSource = resources.and(IoSource.fromFile(clientSpec.getPath))
            val creds = Json.parse(credSource.mkString).as[ClientCreds]

            wsClient.url(TeslaClient.AuthUrl)
                .post(Map(
                    "client_id" -> creds.clientId,
                    "client_secret" -> creds.clientSecret,
                    "email" -> email,
                    "password" -> password,
                    "grant_type" -> "password"
                ))
                .map(response => new TeslaClient(wsClient, email, creds, Json.parse(response.body).as[AuthResponse]))
        })
    }


    protected case class ClientCreds(clientId: String, clientSecret:String)

    protected object ClientCreds {
    
        implicit val reads: Reads[ClientCreds] = Json.reads[ClientCreds]

    }

}