package com.mj.users.processor.like

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{UnLikeComment, decrementLikeCommentCount, deleteCommentLkeFeed}
import com.mj.users.mongo.MongoConnector.remove
import com.mj.users.mongo.PostDao.{feedCollection}
import reactivemongo.bson.document

import scala.concurrent.ExecutionContext.Implicits.global

class UnlikeCommentProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (commentID: String, memberID: String, feedID:String) => {
      val origin = sender()
      val result = UnLikeComment(commentID, memberID).
        map(resp => origin ! responseMessage(commentID, "", updateSuccess))
        val decrese_comment_like_count = decrementLikeCommentCount(commentID)
        val remove_feed = remove(feedCollection, document("_id" -> feedID)) 
      
        result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
