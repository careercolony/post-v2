package com.mj.users.processor.post

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{ReaderFeedRequest, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.updateReaderFeed

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateReaderProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (feedDto: ReaderFeedRequest) => {
      val origin = sender()
      val result = updateReaderFeed(feedDto).map(resp =>
        origin ! responseMessage(feedDto.feedID, "", updateSuccess)

      )

      result.recover {
        case e: Throwable => {
          origin ! responseMessage(feedDto.feedID, e.getMessage, "")
        }
      }
    }
  }
}
