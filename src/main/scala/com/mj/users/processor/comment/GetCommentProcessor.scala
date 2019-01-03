package com.mj.users.processor.comment

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.getComment

import scala.concurrent.ExecutionContext.Implicits.global

class GetCommentProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postID: String) => {
      val origin = sender()
      val result = getComment(postID).map(response =>
        response match {
          case List() => origin ! responseMessage("", noRecordFound, "")
          case _ => origin ! response
        }
      )

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
