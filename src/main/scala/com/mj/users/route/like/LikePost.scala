package com.mj.users.route.like

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
import com.mj.users.notification.NotificationRoom
import org.slf4j.LoggerFactory
import spray.json._

import scala.util.{Failure, Success}

trait LikePost {
  val likePostUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def likePost(system: ActorSystem, notificationRoom: NotificationRoom): Route = {

    val likePostProcessor = system.actorSelection("/*/likePostProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)

    pathPrefix("v1") {
      path("likepost") {
        put {
          entity(as[LikePostRequest]) { dto =>

            val userResponse = likePostProcessor ? (dto, notificationRoom)
            onComplete(userResponse) {
              case Success(resp) =>
                resp match {
                  case s: LikePostResponse => {
                    complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                  }
                  case s: responseMessage =>
                    complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                  case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
                }
              case Failure(error) =>
                likePostUserLog.error("Error is: " + error.getMessage)
                complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
            }

          }
        }
      }
    }
  }
}
