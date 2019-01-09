package com.mj.users.processor.post

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{Feed, PostRequest, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.Neo4jConnector.updateNeo4j
import com.mj.users.mongo.PostDao.{insertNewPost, insertNewPostFeed}
import com.mj.users.notification.{NotificationActor, NotificationRoom}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class NewPostProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postRequestDto: PostRequest  , notificationRoom : NotificationRoom) => {
      val origin = sender()
      val result = insertNewPost(postRequestDto).flatMap(postResponse => {
        insertNewPostFeed(postResponse,"Post").flatMap(insertNewPostFeed => {
          notificationRoom.notificationActor !  insertNewPostFeed
          val script = s"CREATE (s:feeds {memberID:'${postRequestDto.memberID}', FeedID: '${insertNewPostFeed._id}', post_date: TIMESTAMP()})"
          updateNeo4j(script)
        }
          ).map(resp => sendPostToKafka(postRequestDto.toJson.toString)).map(resp => origin ! postResponse)
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
