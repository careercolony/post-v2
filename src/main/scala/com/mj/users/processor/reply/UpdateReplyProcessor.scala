package com.mj.users.processor.reply

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{Reply, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{updateReply}

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateReplyProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (replyDto: Reply) => {
      val origin = sender()
      val result = updateReply(replyDto).map(replyResponse =>
          origin ! responseMessage(replyDto.replyID, "", updateSuccess)
      )

      result.recover {
        case e: Throwable => {
          origin ! responseMessage(replyDto.replyID, e.getMessage, "")
        }
      }
    }
  }
}
