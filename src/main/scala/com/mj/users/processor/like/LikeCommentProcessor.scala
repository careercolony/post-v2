package com.mj.users.processor.like

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.JsonRepo._
import com.mj.users.model._
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{LikeComment, format, getFeedForComment, insertLikeFeedForComment}
import com.mj.users.notification.NotificationRoom
import reactivemongo.bson.BSONDateTime
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class LikeCommentProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (likeCommentRequestDto: LikeCommentRequest, notificationRoom: NotificationRoom) => {
      val origin = sender()
      val result = LikeComment(likeCommentRequestDto).flatMap(resp =>
        getFeedForComment(likeCommentRequestDto)).flatMap(
        resp => insertLikeFeedForComment(resp.get, "liked", likeCommentRequestDto.memberID)
      ).map(response => {
        notificationRoom.notificationActor ! response
        origin ! LikeCommentResponse(likeCommentRequestDto.memberID, likeCommentRequestDto.commentID, likeCommentRequestDto.like, format.format(new java.util.Date(BSONDateTime(System.currentTimeMillis).value)))
      })


      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
