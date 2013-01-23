

name := "scrabbit"

version := "0.1"

scalaVersion := "2.9.1"

mainClass := Some("spidaman.Main")

libraryDependencies ++= Seq(
    "com.typesafe.akka" % "akka-actor" % "2.0.4",
    "com.rabbitmq" % "amqp-client" % "3.0.1"
)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"