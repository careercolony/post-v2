package com.mj.users.route.post

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

trait GetFriendsPostRoute extends PaginationDirectives {
  val getFriendsPostUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def getFriendsPost(system: ActorSystem): Route = {

    implicit val timeout = Timeout(20, TimeUnit.SECONDS)
    val getFriendsPostProcessor = system.actorSelection("/*/getFriendsPostProcessor")
    pathPrefix("v1") {
      path("get-friends-post" / "memberID" / Segment) { memberID =>
        get {
          withPagination { page =>

            val userResponse = getFriendsPostProcessor ? ( memberID  , Option(page))

            onComplete(userResponse) {
              case Success(resp) =>
                resp match {
                  case s: List[Feed] => {
                    println("s:"+s)
                    complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                  }
                  case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
                }
              case Failure(error) =>
                getFriendsPostUserLog.error("Error is: " + error.getMessage)
                complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
            }
          }
        }
      }
    }

  }

}
