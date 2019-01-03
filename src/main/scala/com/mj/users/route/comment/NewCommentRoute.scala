package com.mj.users.route.comment

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

trait NewCommentRoute {
  val newCommentUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def newComment(system: ActorSystem): Route = {

    val newCommentProcessor = system.actorSelection("/*/newCommentProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)

    pathPrefix("v1") {
      path("new-comment") {
        post {
          entity(as[CommentRequest]) { dto =>

            val userResponse = newCommentProcessor ? dto
            onComplete(userResponse) {
              case Success(resp) =>
                resp match {
                  case s: Comment => {
                    complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                  }
                  case s: responseMessage =>
                    complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                  case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
                }
              case Failure(error) =>
                newCommentUserLog.error("Error is: " + error.getMessage)
                complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
            }

          }
        }
      }
    }
  }
}
