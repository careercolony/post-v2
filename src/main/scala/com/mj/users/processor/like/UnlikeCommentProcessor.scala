package com.mj.users.processor.like

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.UnLikeComment

import scala.concurrent.ExecutionContext.Implicits.global

class UnlikeCommentProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (commentID: String, memberID: String) => {
      val origin = sender()
      val result = UnLikeComment(commentID, memberID).
        map(resp => origin ! responseMessage(commentID, "", updateSuccess))

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
