package com.mj.users.processor.comment

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{Comment, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{updateComment}

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateCommentProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (commentDto: Comment) => {
      val origin = sender()
      val result = updateComment(commentDto).map(commentResponse =>
          origin ! responseMessage(commentDto.commentID, "", updateSuccess)
      )

      result.recover {
        case e: Throwable => {
          origin ! responseMessage(commentDto.commentID, e.getMessage, "")
        }
      }
    }
  }
}
