package com.mj.users.route.reply

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{responseMessage, _}
import org.slf4j.LoggerFactory
import spray.json._

import scala.util.{Failure, Success}

trait NewReplyRoute {
  val newReplyUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def newReply(system: ActorSystem): Route = {

    val newReplyProcessor = system.actorSelection("/*/newReplyProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)

    pathPrefix("v1") {
      path("new-reply") {
        post {
          entity(as[ReplyRequest]) { dto =>

            val userResponse = newReplyProcessor ? dto
            onComplete(userResponse) {
              case Success(resp) =>
                resp match {
                  case s: Reply => {
                    complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                  }
                  case s: responseMessage =>
                    complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                  case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
                }
              case Failure(error) =>
                newReplyUserLog.error("Error is: " + error.getMessage)
                complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
            }

          }
        }
      }
    }
  }
}
