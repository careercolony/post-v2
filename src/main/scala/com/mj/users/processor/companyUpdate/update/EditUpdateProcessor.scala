package com.mj.users.processor.companyUpdate.update

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{Update, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{editUpdate, updateUpdateFeed}

import scala.concurrent.ExecutionContext.Implicits.global

class EditUpdateProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postDto: Update) => {
      val origin = sender()
      
      val result = editUpdate(postDto).map(postResponse => {
        println("Hello")
        updateUpdateFeed(postDto, "Update").map(resp =>
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
