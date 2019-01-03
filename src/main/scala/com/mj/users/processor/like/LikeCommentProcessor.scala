package com.mj.users.processor.like

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model._
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.Neo4jConnector.updateNeo4j
import com.mj.users.mongo.PostDao.{LikeComment, insertNewLikeFeed}

import scala.concurrent.ExecutionContext.Implicits.global

class LikeCommentProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (likeCommentRequestDto: LikeCommentRequest) => {
      val origin = sender()
      val result = LikeComment(likeCommentRequestDto).map(resp => origin ! LikeCommentResponse(likeCommentRequestDto.memberID, likeCommentRequestDto.commentID, likeCommentRequestDto.like,  ""))


      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
