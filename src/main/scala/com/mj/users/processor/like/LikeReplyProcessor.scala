package com.mj.users.processor.like

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.JsonRepo._
import com.mj.users.model._
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{LikeReply, format, getFeedForLikeReply, incrementLikeReplyCount, insertReplyFeedForComment}
import com.mj.users.notification.NotificationRoom
import reactivemongo.bson.BSONDateTime
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class LikeReplyProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (likeReplyRequestDto: LikeReplyRequest, notificationRoom: NotificationRoom) => {
      val origin = sender()
      val result = LikeReply(likeReplyRequestDto).flatMap(resp =>
        getFeedForLikeReply(likeReplyRequestDto)).flatMap(
        resp => insertReplyFeedForComment(resp.get, "like_reply", likeReplyRequestDto.actorID)
      ).map(response => {
        notificationRoom.notificationActor ! response
        val reply_like_count = incrementLikeReplyCount(likeReplyRequestDto.replyID)
        
        origin ! LikeReplyResponse(likeReplyRequestDto.actorID, likeReplyRequestDto.replyID, likeReplyRequestDto.like, format.format(new java.util.Date(BSONDateTime(System.currentTimeMillis).value)))
      })

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}






/**
package com.mj.users.processor.like

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.JsonRepo._
import com.mj.users.model._
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{LikeReply, format, incrementLikeReplyCount}
import com.mj.users.notification.NotificationRoom
import reactivemongo.bson.BSONDateTime
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class LikeReplyProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (likeReplyRequestDto: LikeReplyRequest, notificationRoom: NotificationRoom) => {
      val origin = sender()
      val result = LikeReply(likeReplyRequestDto).map(response => {
        
        notificationRoom.notificationActor ! response
        val reply_like_count = incrementLikeReplyCount(likeReplyRequestDto.replyID)
        println(likeReplyRequestDto.replyID)
      
        origin ! LikeReplyResponse(likeReplyRequestDto.memberID, likeReplyRequestDto.replyID, likeReplyRequestDto.like, format.format(new java.util.Date(BSONDateTime(System.currentTimeMillis).value)))
      })


      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
*/