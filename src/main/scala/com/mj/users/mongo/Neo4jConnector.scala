package com.mj.users.mongo

import java.util.concurrent.Executors

import com.mj.users.config.Application._
import org.neo4j.driver.v1.{AuthTokens, GraphDatabase}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object Neo4jConnector {

  implicit val ec: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50))

  val driver = GraphDatabase.driver(neo4jUrl, AuthTokens.basic(neo4jUsername, neo4jPassword))


  def updateNeo4j(script: String): Future[Int] = {

    println("here2")
    val insertResult = for {
      session <- Future {
        driver.session()
      }
      result <- Future {
        session.run(script)
      }
    } yield {
      session.close()
      result.consume().counters().relationshipsCreated() + result.consume().counters().relationshipsDeleted() + result.consume().counters().nodesCreated() + result.consume().counters().propertiesSet()

    }
    insertResult.recover {
      case e: Throwable =>
        println("e" + e.getMessage)
        throw new Exception("Neo4j DB Error")
    }
  }

  def getNeo4j(script: String) = {

    val insertResult = for {
      session <- Future {
        driver.session()
      }
      result <- Future {
        session.run(script)
      }
    } yield {
      session.close()
      result

    }
    insertResult.recover {
      case e: Throwable =>
        println("e" + e.getMessage)
        throw new Exception("Neo4j DB Error")
    }
  }

}