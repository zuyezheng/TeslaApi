package com.zuyezheng.tesla

import java.nio.file.Paths
import java.nio.file.StandardOpenOption.{APPEND, CREATE, WRITE}

import akka.actor.{Actor, ActorSystem, PoisonPill}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Flow, Keep}
import akka.util.ByteString
import com.typesafe.scalalogging.Logger
import com.zuyezheng.tesla.TeslaLogger.Config
import com.zuyezheng.tesla.api.{StreamingFrame, TeslaClient, Vehicle}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Properties, Success}

/**
  * Actor for stream to CSV logging with reconnect and reauthentication.
  *
  * @author zuye.zheng
  */
class TeslaLogger(implicit materializer: ActorMaterializer) extends Actor {
    
    private val logger: Logger = Logger[TeslaLogger]
    
    implicit private val system: ActorSystem = this.context.system
    implicit private val executionContext: ExecutionContext = system.dispatcher
    
    override def receive: Receive = {
        case Config(client, vehicle, retry) =>
            this.logger.info(s"Starting logging for '${vehicle.vin}'.")
            
            // start streaming for the given vehicle
            client.stream(vehicle).runWith(Flow[StreamingFrame]
                // only log values for data updates
                .collect({ case StreamingFrame.Data("data:update", _, value, _) => value })
                .map(_ + Properties.lineSeparator)
                .map(ByteString(_))
                // create or append to an existing file
                .toMat(FileIO.toPath(
                    Paths.get("logs/vehicle_" + vehicle.vin + ".csv"),
                    Set(WRITE, APPEND, CREATE)
                ))(Keep.right)
            ).onComplete {
                case Success(_) =>
                    this.logger.info(s"Stream closed, reconnecting for '${vehicle.vin}'.")
                    this.self ! Config(client, vehicle)
                    
                case Failure(e) =>
                    this.logger.error(s"Logging error, will try to recover for '${vehicle.vin}': ${e.getMessage}")
                    
                    // failed, probably due to an expired token, try to re-authenticate
                    client.refresh(vehicle).onComplete {
                        case Success((refreshedClient, Some(refreshedVehicle))) =>
                            // use the refreshed client to start streaming again
                            this.logger.info(s"Reauthenticated, retry $retry logging for '${refreshedVehicle.vin}'.")
                            this.context.system.scheduler.scheduleOnce(
                                // if for some reason we can re-auth, but error out streaming, do an exponential backoff
                                (retry ^ 2) second,
                                this.self,
                                Config(refreshedClient, refreshedVehicle, retry + 1)
                            )
                            
                        case _ =>
                            // stop logging if can't re-auth
                            this.logger.error(s"Could not reauthenticate, stopping logging for '${vehicle.vin}'.")
                            this.self ! PoisonPill
                    }
            }
    }
    
}

object TeslaLogger {
    
    case class Config(client: TeslaClient, vehicle: Vehicle, retry: Int = 0)
    
}