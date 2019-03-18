name := "TeslaApi"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.7"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.21"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.21"

libraryDependencies += "com.typesafe.play" %% "play-ws-standalone" % "2.0.2"
libraryDependencies += "com.typesafe.play" %% "play-ws-standalone" % "2.0.2"

val playStandaloneVersion = "2.0.2"
libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-ahc-ws-standalone" % playStandaloneVersion,
    "com.typesafe.play" %% "play-ws-standalone-json" % playStandaloneVersion
)