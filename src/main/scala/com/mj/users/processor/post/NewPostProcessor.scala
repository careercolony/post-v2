package com.mj.users.processor.post

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{PostRequest, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.Neo4jConnector.updateNeo4j
import com.mj.users.mongo.PostDao.{insertNewPost, insertNewPostFeed}
import com.mj.users.notification.NotificationRoom
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class NewPostProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postRequestDto: PostRequest, notificationRoom: NotificationRoom) => {
      val origin = sender()
      val result = insertNewPost(postRequestDto).flatMap(postResponse => {
        println(postResponse)
        insertNewPostFeed(postResponse, "Post").flatMap(feedResponse => {
          println(feedResponse.toJson.toString)
          notificationRoom.notificationActor ! feedResponse
          sendPostToKafka(feedResponse.toJson.toString)
          val script = s"CREATE (s:feeds {memberID:'${postRequestDto.memberID}', FeedID: '${feedResponse._id}', post_date: TIMESTAMP()})"
          updateNeo4j(script)
        }
        ).map(resp => {
      
        }).map(resp => origin ! postResponse)
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
