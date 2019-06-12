package com.mj.users.tools

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.mj.users.notification.NotificationRoom
import com.mj.users.route.comment.{GetCommentCountRoute, GetCommentRoute, NewCommentRoute}
import com.mj.users.route.experience._
import com.mj.users.route.like._
import com.mj.users.route.notification.{NotificationService, UpdateFeedReaders}
import com.mj.users.route.post._
import com.mj.users.route.companyUpdate.update._
import com.mj.users.route.companyUpdate.update.{NewJobFeedRoute}
import com.mj.users.route.reply.{GetRepliesRoute, NewReplyRoute}
import org.joda.time.DateTime
import com.mj.users.config.Application._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object RouteUtils extends NewPostRoute with UpdatePostRoute with NewCommentRoute with GetCommentRoute
  with NewUpdateRoute with EditUpdateRoute with GetOneUpdateRoute with GetUpdateByMemberRoute with DeleteUpdateRoute with LikePost with UnlikePost with GetAllPostRoute 
  with NewJobFeedRoute with GetCommentCountRoute with GetMemberIDPostRoute with GetFriendsPostRoute with GetFriendsNotifyFeedRoute with SharePostRoute
  with LikeComment with UnlikeComment with NotificationService with UpdateFeedReaders 
  with DeleteCommentRoute with DeletePostRoute
  with NewReplyRoute with GetRepliesRoute with GetAllLikesRoute {


  /*  createUsersCollection()
    createOnlinesCollection()*/

  def badRequest(request: HttpRequest): StandardRoute = {
    val method = request.method.value.toLowerCase
    val path = request.getUri().path()
    //  val queryString = request.getUri().rawQueryString().orElse("")
    method match {
      case _ =>
        complete((StatusCodes.NotFound, "404 error, resource not found!"))
    }
  }

  //log duration and request info route
  def logDuration(inner: Route)(implicit ec: ExecutionContext): Route = { ctx =>
    val rejectionHandler = RejectionHandler.default
    val start = System.currentTimeMillis()
    val innerRejectionsHandled = handleRejections(rejectionHandler)(inner)
    mapResponse { resp =>
      val currentTime = new DateTime()
      val currentTimeStr = currentTime.toString("yyyy-MM-dd HH:mm:ss")
      val duration = System.currentTimeMillis() - start
      var remoteAddress = ""
      var userAgent = ""
      var rawUri = ""
      ctx.request.headers.foreach(header => {
        //this setting come from nginx
        if (header.name() == "X-Real-Ip") {
          remoteAddress = header.value()
        }
        if (header.name() == "User-Agent") {
          userAgent = header.value()
        }
        //you must set akka.http.raw-request-uri-header=on config
        if (header.name() == "Raw-Request-URI") {
          rawUri = header.value()
        }
      })
      Future {
        val mapPattern = Seq("user")
        var isIgnore = false
        mapPattern.foreach(pattern =>
          isIgnore = isIgnore || rawUri.startsWith(s"/$pattern"))
        if (!isIgnore) {
          println(
            s"# $currentTimeStr ${ctx.request.uri} [$remoteAddress] [${ctx.request.method.name}] [${resp.status.value}] [$userAgent] took: ${duration}ms")
        }
      }
      resp
    }(innerRejectionsHandled)(ctx)
  }

  def routeRoot(notificationRoom: NotificationRoom,
                system: ActorSystem,
                materializer: ActorMaterializer) = {
    pathPrefix("post" / version) { routeLogic(notificationRoom,system,materializer) }~
      extractRequest { request =>
        badRequest(request)
      }
  }


  def routeLogic( notificationRoom: NotificationRoom,
                 system: ActorSystem,
                 materializer: ActorMaterializer) = {
    //val notificationRoom: NotificationRoom = new NotificationRoom(system)
    println("notificationRoom:"+notificationRoom)
    newPost(system, notificationRoom) ~ updatePost(system) ~ newComment(system, notificationRoom) ~ getComment(system) ~
      getFriendsNotifyFeeds(system) ~ newUpdate(system,notificationRoom) ~ editUpdate(system) ~ getOneUpdate(system) ~ getUpdateByMember(system) ~ deleteUpdate(system) ~ likePost(system, notificationRoom) ~ unLikePost(system) ~ getAllPost(system) ~ getCommentCount(system) ~ getMemberIDPost(system) ~ getFriendsPost(system) ~
      newJobFeed(system,notificationRoom) ~ sharePost(system, notificationRoom) ~ likeComment(system, notificationRoom) ~ unLikeComment(system) ~ notification(system, notificationRoom) ~
      updateReader(system) ~ deletePost(system) ~ deleteComment(system) ~ newReply(system) ~ getRepliesRoute(system) ~ getAllLikes(system)

  }

  def logRoute(notificationRoom: NotificationRoom,
               system: ActorSystem,
               materializer: ActorMaterializer) = logDuration(routeRoot(notificationRoom,system,materializer))
}
