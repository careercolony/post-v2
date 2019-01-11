package com.mj.users.route.like

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{complete, path, _}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{responseMessage, _}
import com.mj.users.mongo.PostDao.getAllLikesStore
import org.slf4j.LoggerFactory
import spray.json._

import scala.util.{Failure, Success}

trait GetAllLikesRoute extends MessageConfig {
  val getAllLikesUserLog = LoggerFactory.getLogger(this.getClass.getName)


  def getAllLikes(system: ActorSystem): Route = {

    implicit val timeout = Timeout(20, TimeUnit.SECONDS)

    pathPrefix("v1") {
      path("get-likes" / "postID" / Segment) { postID =>
        get {
          val userResponse = getAllLikesStore(postID)

          onComplete(userResponse) {
            case Success(resp) =>
              resp match {
                case Nil =>
                  complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", noRecordFound, "").toJson.toString)))
                case s: List[Post] => {
                  val postLike = s.head.likes
                  if (postLike.isDefined) {
                    val postResponse = postLike.get.map(response => LikePostResponse(s.head.memberID, response.likeID, s.head.postID, response.like, response.like_date))
                    complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, postResponse.toJson.toString)))
                  } else
                    complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", noRecordFound, "").toJson.toString)))
                }
                case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
              }
            case Failure(error) =>
              getAllLikesUserLog.error("Error is: " + error.getMessage)
              complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
          }

        }
      }
    }

  }

}
