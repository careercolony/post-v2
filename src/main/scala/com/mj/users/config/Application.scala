package com.mj.users.config

import com.typesafe.config.{Config, ConfigFactory}
import reactivemongo.api.{MongoConnection, MongoDriver}

import scala.concurrent.Future

object Application {
  val config: Config = ConfigFactory.load("application.conf")

  val configServer: Config = config.getConfig("server")
  val hostName: String = configServer.getString("hostName")
  val port: Int = configServer.getString("port").toInt
  val akkaPort: Int = configServer.getString("akkaPort").toInt
  val seedNodes: String = configServer.getString("seedNodes")
  val poolSize: Int = config.getInt("poolSize")

  val configMongo: Config = config.getConfig("mongodb")
  val configMongoDbname: String = configMongo.getString("dbname")
  var configMongoUri: String = configMongo.getString("uri")

  //neo4j config
  val neo4jUrl = config.getString("neo4j.url")
  val neo4jUsername = config.getString("neo4j.username")
  var neo4jPassword = config.getString("neo4j.password")


  val kongAdminURL = config.getString("kong.admin.url")
  val kongExpirationTime = config.getInt("kong.expiration.time")

  val active: String = config.getString("status.active")
  val deleted: String = config.getString("status.deleted")


  val version = config.getString("postVersion")
  //Mongo configuration
  val dbName = configMongoDbname

  val mongoUri = Application.configMongoUri
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection)
  val futureConnection = Future.fromTry(connection)

  val topic: String = config.getString("kafka.topic")
  val brokers: String = config.getString("kafka.brokers")
}
