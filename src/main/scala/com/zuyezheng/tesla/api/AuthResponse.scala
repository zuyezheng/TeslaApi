package com.zuyezheng.tesla.api

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Json, JsonConfiguration, Reads}

/**
  * @author zuye.zheng
  */
case class AuthResponse(
    accessToken: String,
    tokenType: String,
    expiresIn: Int,
    refreshToken: String,
    createdAt: Int
)

object AuthResponse {
    
    implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
    implicit val userReads: Reads[AuthResponse] = Json.reads[AuthResponse]
    
}
