package com.zuyezheng.tesla.api

/**
  * Available properties for streaming.
  *
  * @author zuye.zheng
  */
object StreamingProperty extends Enumeration {

    protected case class Val(key: String) extends super.Val(key)

    lazy val keys: Seq[String] = StreamingProperty.values.toSeq.map(_.asInstanceOf[Val].key)

    val Speed = Val("speed")
    val Odometer = Val("odometer")
    val StateOfCharge = Val("soc")
    val Elevation = Val("elevation")
    val EstimatedLat = Val("est_lat")
    val EstimatedLng = Val("est_lng")
    val Power = Val("power")
    val ShiftState = Val("shift_state")
    val Range = Val("range")
    val EstimatedRange = Val("est_range")
    val Heading = Val("heading")
    val EstimatedHeading = Val("est_heading")

}

