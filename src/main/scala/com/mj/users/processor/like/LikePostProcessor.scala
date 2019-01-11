package com.mj.users.processor.like

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{LikePostRequest, LikePostResponse, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.Neo4jConnector.updateNeo4j
import com.mj.users.mongo.PostDao.{LikePost, format, insertNewLikeFeed}
import com.mj.users.notification.NotificationRoom
import reactivemongo.bson.BSONDateTime

import scala.concurrent.ExecutionContext.Implicits.global

class LikePostProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (likePostRequestDto: LikePostRequest, notificationRoom: NotificationRoom) => {
      val origin = sender()
      val result = LikePost(likePostRequestDto).flatMap(likePostResponse =>
        insertNewLikeFeed(likePostRequestDto, "liked")).map(resp => {
        notificationRoom.notificationActor ! resp
        val script = s"CREATE (s:feeds {memberID:'${likePostRequestDto.memberID}', FeedID: '${resp._id}', likePost_date: TIMESTAMP()})"
        updateNeo4j(script)

      }).map(resp => origin ! LikePostResponse(likePostRequestDto.memberID, likePostRequestDto.actorID, likePostRequestDto.postID, likePostRequestDto.like,  format.format(new java.util.Date(BSONDateTime(System.currentTimeMillis).value))))


      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
