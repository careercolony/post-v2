package com.mj.users.processor.post

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model._
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.updateNewSharePost
import com.mj.users.notification.NotificationRoom

import scala.concurrent.ExecutionContext.Implicits.global

class SharePostProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postDto: PostShare, notificationRoom: NotificationRoom) => {
      val origin = sender()
      val result = updateNewSharePost(postDto).map(postResponse => {
        // notificationRoom.notificationActor ! 1
        origin ! responseMessage(postDto.postID, "", updateSuccess)
      })


      result.recover {
        case e: Throwable => {
          origin ! responseMessage(postDto.postID, e.getMessage, "")
        }
      }
    }
  }
}
