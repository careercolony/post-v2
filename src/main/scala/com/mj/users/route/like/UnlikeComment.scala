package com.mj.users.route.like

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, path, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.mj.users.model.JsonRepo._
import com.mj.users.model.responseMessage
import org.slf4j.LoggerFactory
import spray.json._

import scala.util.{Failure, Success}

trait UnlikeComment {
  val unLikeCommentUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def unLikeComment(system: ActorSystem): Route = {

    val unLikeCommentProcessor = system.actorSelection("/*/unLikeCommentProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)


    path("unlike-comment" / "commentID" / Segment / "memberID" / Segment) { (commentID: String, memberID: String) =>
      get {
        val userResponse = unLikeCommentProcessor ? (commentID, memberID)
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
            unLikeCommentUserLog.error("Error is: " + error.getMessage)
            complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
        }

      }
    }
  }

}

