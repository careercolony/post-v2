package com.mj.users.processor.like

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{UnLikePost, decrementCommentCount}


import scala.concurrent.ExecutionContext.Implicits.global

class UnlikPostProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postID: String, memberID: String) => {
      val origin = sender()
      val result = UnLikePost(postID, memberID).
        map(resp => origin ! responseMessage(postID, "", updateSuccess))

        val remove_like_post_count = decrementCommentCount(postID)

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
