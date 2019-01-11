package com.mj.users.processor.post

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.MongoConnector.remove
import com.mj.users.mongo.PostDao.postCollection
import reactivemongo.bson.document

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeletePostProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postID: String) => {
      val origin = sender()

      val result = remove(postCollection, document("postID" -> postID))
        .flatMap(upResult => Future {
          responseMessage("", "", deleteSuccess)
        }).map(response => origin ! response)

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
