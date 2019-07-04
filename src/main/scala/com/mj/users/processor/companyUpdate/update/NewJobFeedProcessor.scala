package com.mj.users.processor.companyUpdate.update

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{JobRequest, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{getJobFromCompany, insertNewJobFeed}
import com.mj.users.mongo.Neo4jConnector.updateNeo4j
import com.mj.users.notification.NotificationRoom
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global



class NewJobFeedProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  

def receive = {

    case (jobRequestDto: JobRequest, notificationRoom: NotificationRoom) => {
      val origin = sender()
      val result = getJobFromCompany(jobRequestDto).flatMap(postResponse => {
        println(postResponse)
        insertNewJobFeed(postResponse, "Job").flatMap(feedResponse => {
          println(feedResponse.toJson.toString)
          notificationRoom.notificationActor ! feedResponse
          sendPostToKafka(feedResponse.toJson.toString)
          val script = s"CREATE (s:feeds {memberID:'${jobRequestDto.memberID}',  FeedID: '${feedResponse._id}', post_date: TIMESTAMP()}) "
          updateNeo4j(script)
        }).map(resp => {
            
        }).map(resp => origin ! jobRequestDto)
      })
    
      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }

/**
  def receive = {

    case (postRequestDto: JobRequest, notificationRoom: NotificationRoom) => {
      
      val origin = sender()
      val result = insertNewJobFeed(postRequestDto).flatMap(feedResponse => {
          println("Hello"+feedResponse)
          notificationRoom.notificationActor ! feedResponse
          //sendPostToKafka(feedResponse.toJson.toString)
          
          val script = s"CREATE (s:feeds {memberID:'${postRequestDto.memberID}', FeedID: '${feedResponse._id}', post_date: TIMESTAMP()})"
          updateNeo4j(script)
       
          
            //println("postResponse:"+postResponse.toJson.toString)
          //sendPostToKafka(postResponse.toJson.toString)
        }).map(resp => origin ! postRequestDto)
      

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
*/

/**
  def receive = {

    case (postRequestDto: JobRequest, notificationRoom: NotificationRoom) => {
      val origin = sender()
      val result = insertNewJob(postRequestDto).flatMap(postResponse => {
          insertNewJobFeed(postResponse, "job").flatMap(feedResponse => {
           println(feedResponse)
           println(postResponse)
          notificationRoom.notificationActor ! feedResponse
          sendPostToKafka(feedResponse.toJson.toString)

         
          
          val script = s"CREATE (s:feeds {memberID:'${postRequestDto.memberID}', FeedID: '${feedResponse._id}', post_date: TIMESTAMP()})"
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
*/
}

