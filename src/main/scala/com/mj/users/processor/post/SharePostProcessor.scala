package com.mj.users.processor.post

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model._
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.Neo4jConnector.updateNeo4j
import com.mj.users.mongo.PostDao.{updateNewSharePost,insertNewShareFeed}
import com.mj.users.notification.NotificationRoom
import com.mj.users.model.JsonRepo._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global

class SharePostProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (postDto: PostShare, notificationRoom: NotificationRoom) => {
      val origin = sender()
        
        val result = updateNewSharePost(postDto).map(postResponse => {
            insertNewShareFeed(postDto, "Shared").flatMap(resp => {
            notificationRoom.notificationActor ! resp
            sendPostToKafka(resp.toJson.toString)
            
            println(resp)
            val script = s"CREATE (s:feeds {memberID:'${postDto.actorID}', FeedID: '${resp._id}', comment_date: TIMESTAMP()})"
            updateNeo4j(script)
            
            }).map(resp => origin ! postResponse)
           
          })

          result.recover {
            case e: Throwable => {
              origin ! responseMessage(postDto.postID, e.getMessage, "")
            }
          }
      
   
    }
  }
}