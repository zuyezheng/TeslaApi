package com.zuyezheng.tesla

import java.nio.file.Paths
import java.nio.file.StandardOpenOption.{APPEND, CREATE, WRITE}

import akka.actor.{Actor, ActorSystem, PoisonPill}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Flow, Keep}
import akka.util.ByteString
import com.zuyezheng.tesla.TeslaLogger.Config
import com.zuyezheng.tesla.api.{StreamingFrame, TeslaClient, Vehicle}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Properties, Success}

/**
  * Actor that does the logging and re-authenticates when needed.
  *
  * @author zuye.zheng
  */
class TeslaLogger(implicit materializer: ActorMaterializer) extends Actor {
    
    implicit private val system: ActorSystem = this.context.system
    implicit private val executionContext: ExecutionContext = system.dispatcher
    
    override def receive: Receive = {
        case Config(client, vehicle, retry) => client.stream(vehicle)
            .runWith(Flow[StreamingFrame]
                // only log values for data updates
                .collect({ case StreamingFrame.Data("data:update", _, value, _) => value })
                .map(_ + Properties.lineSeparator)
                .map(ByteString(_))
                .toMat(FileIO.toPath(
                    Paths.get("logs/vehicle_" + vehicle.vin + ".csv"),
                    Set(WRITE, APPEND, CREATE)
                ))(Keep.right)
            ).onComplete {
            case Success(_) =>
                // finished successfully, keep streaming
                this.self ! Config(client, vehicle)
            case Failure(_) =>
                // failed, probably due to an expired token, try to re-authenticate
                client.refresh(vehicle).onComplete {
                    case Success((refreshedClient, Some(refreshedVehicle))) =>
                        // use the refreshed client to start streaming again
                        this.context.system.scheduler.scheduleOnce(
                            // if for some reason we can re-auth, but error out streaming, do an exponential backoff
                            (retry ^ 2) second,
                            this.self,
                            Config(refreshedClient, refreshedVehicle, retry + 1)
                        )
                    case _ =>
                        // stop logging if can't re-auth
                        this.self ! PoisonPill
                }
        }
    }
    
}

object TeslaLogger {
    
    case class Config(client: TeslaClient, vehicle: Vehicle, retry: Int = 0)
    
}