package com.mj.users.route.post

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import com.mj.users.directive.PaginationDirectives
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{responseMessage, _}
import com.mj.users.mongo.PostDao.getMemberIDStore
import org.slf4j.LoggerFactory
import spray.json._

import scala.util.{Failure, Success}


trait GetMemberIDPostRoute extends Directives with PaginationDirectives {
  val getMemberIDUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def getMemberIDPost(system: ActorSystem): Route = {

    implicit val timeout = Timeout(20, TimeUnit.SECONDS)


    path("get-post" / "memberID" / Segment) { memberID =>
      get {
        withPagination { page =>
          val userResponse = getMemberIDStore(memberID, page)
          onComplete(userResponse) {
            case Success(resp) =>
              resp match {
                case s: List[Post] => {
                  complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                }
                case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
              }
            case Failure(error) =>
              getMemberIDUserLog.error("Error is: " + error.getMessage)
              complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
          }
        }
      }
    }
  }

}

