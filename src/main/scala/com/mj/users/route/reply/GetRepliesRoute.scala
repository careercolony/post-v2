package com.mj.users.route.reply

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, path, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.mj.users.directive.PaginationDirectives
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{responseMessage, _}
import org.slf4j.LoggerFactory
import spray.json._

import scala.util.{Failure, Success}

trait GetRepliesRoute extends PaginationDirectives {
  val getRepliesUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def getRepliesRoute(system: ActorSystem): Route = {

    implicit val timeout = Timeout(20, TimeUnit.SECONDS)
    val getRepliesProcessor = system.actorSelection("/*/getRepliesProcessor")
    pathPrefix("v1") {
      path("get-replies" / "commentID" / Segment) { commentID =>
        get {

          val userResponse = getRepliesProcessor ? commentID

          onComplete(userResponse) {
            case Success(resp) =>
              resp match {
                case s: List[Reply] => {
                  println("s:" + s)
                  complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                }
                case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
              }
            case Failure(error) =>
              getRepliesUserLog.error("Error is: " + error.getMessage)
              complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
          }

        }
      }
    }

  }

}
