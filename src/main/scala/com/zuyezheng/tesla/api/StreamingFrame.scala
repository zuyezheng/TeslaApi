package com.zuyezheng.tesla.api

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Json, JsonConfiguration, Reads}

/**
  * Frame from the streaming response.
  *
  * @author zuye.zheng
  */
abstract class StreamingFrame(msgType: String)

object StreamingFrame {
    
    case class Control(
        msgType: String,
        connectionTimeout: Int
    ) extends StreamingFrame(msgType)
    
    object Control {
        
        implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
        implicit val reads: Reads[Control] = Json.reads[Control]
        
    }
    
    
    case class Data(
        msgType: String,
        tag: String,
        value: String,
        errorType: Option[String]
    ) extends StreamingFrame(msgType)
    
    object Data {
    
        implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
        implicit val reads: Reads[Data] = Json.reads[Data]
        
    }
    
    
    case class Unknown(msgType: String) extends StreamingFrame(msgType)
    
    object Unknown {
        
        implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
        implicit val reads: Reads[Unknown] = Json.reads[Unknown]
        
    }
    
}