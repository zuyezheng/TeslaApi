package com.zuyezheng.tesla

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import ch.qos.logback.classic.{Level, Logger}
import com.zuyezheng.tesla.api.TeslaClient
import org.slf4j.LoggerFactory
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Command line entry point to do some logging.
  *
  * Tesla -u <username> -p <password>
  *
  * @author zuye.zheng
  */
object Tesla {
    
    def main(args: Array[String]): Unit = {
        LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger].setLevel(Level.INFO)
        
        parseArgs(args.toList) match {
            case Some((username, password)) =>
                implicit val system: ActorSystem = ActorSystem("teslaLogger")
                implicit val materializer: ActorMaterializer = ActorMaterializer()
                
                val wsClient = StandaloneAhcWSClient()
                
                // try to authenticate with username and password
                for(
                    client <- TeslaClient(wsClient, "client.json", username, password);
                    vehicles <- client.vehicles
                ) {
                    system.actorOf(
                        Props(classOf[TeslaLogger], materializer),
                        "vehicle_" + vehicles.head.vin
                    ) ! TeslaLogger.Config(client, vehicles.head)
                }
            case _ => throw new RuntimeException("Missing username(-u) and/or password(-p).")
        }
    }
    
    private def parseArgs(argList: List[String]): Option[(String, String)] = {
        var username: Option[String] = None
        var password: Option[String] = None
        
        @tailrec
        def fn(argList: List[String]): Unit = argList match {
            case Nil => Unit
            case "-u" :: v :: tail =>
                username = Some(v)
                fn(tail)
            case "-p" :: v :: tail =>
                password = Some(v)
                fn(tail)
            case a :: tail =>
                throw new RuntimeException(s"Unknown argument ${a}")
        }
        
        fn(argList)
        
        username.flatMap(a => password.map((a, _)))
    }
    
}
