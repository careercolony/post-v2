package com.mj.users.processor.like

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{UnLikeReply, decrementReplyLikeCount}

import scala.concurrent.ExecutionContext.Implicits.global

class UnlikeReplyProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (replyID: String, memberID: String) => {
      val origin = sender()
      val result = UnLikeReply(replyID, memberID).
        map(resp => origin ! responseMessage(replyID, "", updateSuccess))
        val reply_like_count = decrementReplyLikeCount(replyID)
      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
