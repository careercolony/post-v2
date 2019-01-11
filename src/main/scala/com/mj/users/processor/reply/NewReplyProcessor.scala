package com.mj.users.processor.reply

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model._
import com.mj.users.mongo.PostDao.insertNewReply

import scala.concurrent.ExecutionContext.Implicits.global

class NewReplyProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (replyRequest: ReplyRequest) => {
      val origin = sender()
      val result = insertNewReply(replyRequest).map(resp => origin ! resp)

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
