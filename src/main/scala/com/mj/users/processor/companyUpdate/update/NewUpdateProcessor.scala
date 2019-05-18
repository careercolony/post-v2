package com.mj.users.processor.companyUpdate.update

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{UpdateRequest, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{insertNewUpdate, insertNewUpdateFeed}
import com.mj.users.mongo.Neo4jConnector.updateNeo4j
import com.mj.users.notification.NotificationRoom
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class NewUpdateProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postRequestDto: UpdateRequest, notificationRoom: NotificationRoom) => {
      val origin = sender()
      val result = insertNewUpdate(postRequestDto).flatMap(postResponse => {
          insertNewUpdateFeed(postResponse, "Update").flatMap(insertNewUpdateFeed => {
          notificationRoom.notificationActor ! insertNewUpdateFeed
          sendPostToKafka(insertNewUpdateFeed.toJson.toString)
          
          val script = s"CREATE (s:feeds {memberID:'${postRequestDto.memberID}', FeedID: '${insertNewUpdateFeed._id}', post_date: TIMESTAMP()})"
          updateNeo4j(script)
          
        }
        ).map(resp => {
            //println("postResponse:"+postResponse.toJson.toString)
          //sendPostToKafka(postResponse.toJson.toString)
        }).map(resp => origin ! postResponse)
      })

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
