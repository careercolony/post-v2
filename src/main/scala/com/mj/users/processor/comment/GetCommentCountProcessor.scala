package com.mj.users.processor.comment

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.getCommentCount

import scala.concurrent.ExecutionContext.Implicits.global

class GetCommentCountProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postID: String) => {
      val origin = sender()
      val result = getCommentCount(postID).map(response =>
        response match {
          case List() => origin ! responseMessage(postID, noRecordFound, "")
          case _ => origin ! responseMessage(postID,"" , response.size.toString)
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
