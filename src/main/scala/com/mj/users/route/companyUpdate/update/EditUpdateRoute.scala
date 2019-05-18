package com.mj.users.route.companyUpdate.update

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{responseMessage, _}
import org.slf4j.LoggerFactory
import spray.json._

import scala.util.{Failure, Success}

trait EditUpdateRoute {
  val editUpdateUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def editUpdate(system: ActorSystem): Route = {

    val editUpdateProcessor = system.actorSelection("/*/editUpdateProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)


    path("edit-update") {
      put {
        entity(as[Update]) { dto =>

          val userResponse = editUpdateProcessor ? dto
          onComplete(userResponse) {
            case Success(resp) =>
              resp match {
                case s: responseMessage => if (s.successmsg.nonEmpty)
                  complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                else
                  complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
              }
            case Failure(error) =>
              editUpdateUserLog.error("Error is: " + error.getMessage)
              complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
          }

        }
      }
    }
  }
}
