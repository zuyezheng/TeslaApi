package com.zuyezheng.tesla.api

import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, __}

case class Vehicle(
    id: Long,
    vehicleId: Int,
    vin: String,
    displayName: String,
    state: String,
    private val _optionCodes: String,
    tokens: List[String]
) {

    def optionCodes:Array[String] = {
        this._optionCodes.split(",")
    }

}

object Vehicle {

    implicit val reads: Reads[Vehicle] = (
        (__ \ "id").read[Long] and
        (__ \ "vehicle_id").read[Int] and
        (__ \ "vin").read[String] and
        (__ \ "display_name").read[String] and
        (__ \ "state").read[String] and
        (__ \ "option_codes").read[String] and
        (__ \ "tokens").read[List[String]]
    )(Vehicle.apply _)

}
