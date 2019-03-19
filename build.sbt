name := "TeslaApi"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.7"

val akkaStreamVersion = "2.5.21"
libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaStreamVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion
)

val playStandaloneVersion = "2.0.2"
libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-ahc-ws-standalone" % playStandaloneVersion,
    "com.typesafe.play" %% "play-ws-standalone-json" % playStandaloneVersion
)

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"