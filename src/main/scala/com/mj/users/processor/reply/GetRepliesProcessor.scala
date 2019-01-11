package com.mj.users.processor.reply

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.PostDao.getreply

import scala.concurrent.ExecutionContext.Implicits.global

class GetRepliesProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (commentID: String) => {
      val origin = sender()
      val result = getreply(commentID).map(response =>
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
