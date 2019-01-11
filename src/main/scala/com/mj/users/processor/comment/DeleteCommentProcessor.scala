package com.mj.users.processor.comment

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.MongoConnector.remove
import com.mj.users.mongo.PostDao.commentCollection
import reactivemongo.bson.document

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteCommentProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (commentID: String) => {
      val origin = sender()

      val result = remove(commentCollection, document("commentID" -> commentID))
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