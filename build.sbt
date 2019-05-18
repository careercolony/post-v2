enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

packageName in Docker := "careercolony/akka-http-docker-feeds-api"
dockerBaseImage := "openjdk:jre-alpine"

name := "Post"

version := "0.0.1"
scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.5.16"
  val akkaHttpV = "10.1.6"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.0",
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.0-M1",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "com.typesafe.akka" %% "akka-http-experimental" % "2.0-M1",
    "com.typesafe.akka" %% "akka-http-testkit-experimental" % "2.0-M1" % "test",
    "com.typesafe.akka" %% "akka-testkit" % "2.4.0" % "test",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "org.reactivemongo" %% "reactivemongo" %  "0.12.7",
    "org.reactivemongo" %% "reactivemongo-play-json" %  "0.12.7-play26",
    "joda-time" % "joda-time" % "2.8.1",
    "org.neo4j.driver" % "neo4j-java-driver" % "1.0.4",
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.11-M2",
  "com.typesafe.akka" %% "akka-cluster" % akkaV,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaV
  )
}
