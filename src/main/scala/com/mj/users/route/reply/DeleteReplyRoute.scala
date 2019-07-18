package com.mj.users.route.reply

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

trait DeleteReplyRoute {
  val deleteReplyUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def deleteReply(system: ActorSystem): Route = {

    val deleteReplyProcessor = system.actorSelection("/*/deleteReplyProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)


    path("delete-reply" / "replyID" / Segment / "feedID" / Segment) { (replyId: String, feedID: String) =>
      get {


        val userResponse = deleteReplyProcessor ? (replyId, feedID)
        onComplete(userResponse) {
          case Success(resp) =>
            resp match {
              case s: responseMessage => if (s.successmsg.nonEmpty) {
                complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
              } else
                complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
              case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
            }
          case Failure(error) =>
            deleteReplyUserLog.error("Error is: " + error.getMessage)
            complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
        }


      }
    }
  }

}
