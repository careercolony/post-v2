package com.mj.users.processor.post

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{Post, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{updateNewFeed, updateNewPost}

import scala.concurrent.ExecutionContext.Implicits.global

class UpdatePostProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postDto: Post) => {
      val origin = sender()
      val result = updateNewPost(postDto).map(postResponse => {
        updateNewFeed(postDto, "Post").map(resp =>
          origin ! responseMessage(postDto.postID, "", updateSuccess))
      }
      )

      result.recover {
        case e: Throwable => {
          origin ! responseMessage(postDto.postID, e.getMessage, "")
        }
      }
    }
  }
}
