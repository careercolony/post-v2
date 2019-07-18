
package com.mj.users.processor.reply

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.JsonRepo._
import com.mj.users.model._
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{ReplyComment, format, getFeedForCommentReply, insertReplyFeedForComment, incrementReplyCommentCount}
import com.mj.users.notification.NotificationRoom
import reactivemongo.bson.BSONDateTime
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class NewReplyProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (replyRequestDto: ReplyCommentRequest, notificationRoom: NotificationRoom) => {
      val origin = sender()
      val result = ReplyComment(replyRequestDto).flatMap(resp => 
        getFeedForCommentReply(replyRequestDto)).flatMap(
        resp => insertReplyFeedForComment(resp.get, "reply_comment", replyRequestDto.actorID)
      ).map(response => {
        notificationRoom.notificationActor ! response
        val comment_reply_count = incrementReplyCommentCount(replyRequestDto)
      
        origin ! ReplyCommentRespone(replyRequestDto.actorID, replyRequestDto.commentID, replyRequestDto.actorName, replyRequestDto.actorHeadline, replyRequestDto.actorAvatar, replyRequestDto.reply_body, format.format(new java.util.Date(BSONDateTime(System.currentTimeMillis).value)))
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
package com.mj.users.processor.reply

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model._
import com.mj.users.mongo.PostDao.{ReplyComment,incrementReplyCount}

import scala.concurrent.ExecutionContext.Implicits.global

class NewReplyProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (replyRequest: ReplyCommentRequest) => {
      val origin = sender()

      val result = ReplyComment(replyRequest).map(resp => origin ! resp)
      //println(resp)
      //val res1 = getFeedForCommentReply(commentRequestDto, "Comment", commentResponse).flatMap(response => {
         //notificationRoom.notificationActor ! response
         //sendPostToKafka(response.toJson.toString)
      //})
      val comment_reply_count = incrementReplyCount(replyRequest)
      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
*/

