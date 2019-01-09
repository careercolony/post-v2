package com.mj.users.processor.comment

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{CommentRequest, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.Neo4jConnector.updateNeo4j
import com.mj.users.mongo.PostDao.{insertNewComment, insertNewCommentFeed}
import com.mj.users.notification.NotificationRoom

import scala.concurrent.ExecutionContext.Implicits.global

class NewCommentProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (commentRequestDto: CommentRequest , notificationRoom : NotificationRoom) => {
      val origin = sender()
      val result = insertNewComment(commentRequestDto).flatMap(commentResponse => {
        insertNewCommentFeed(commentRequestDto, "Comment").flatMap(resp => {
          notificationRoom.notificationActor !  resp
          val script = s"CREATE (s:feeds {memberID:'${commentRequestDto.memberID}', FeedID: '${resp._id}', comment_date: TIMESTAMP()})"
          updateNeo4j(script)

        }).map(resp => origin ! commentResponse)
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
