package com.mj.users.processor.reply

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model._
import com.mj.users.mongo.PostDao.{insertNewReply,incrementReplyCount}

import scala.concurrent.ExecutionContext.Implicits.global

class NewReplyProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (replyRequest: ReplyRequest) => {
      val origin = sender()

      val result = insertNewReply(replyRequest).map(resp => origin ! resp)
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


