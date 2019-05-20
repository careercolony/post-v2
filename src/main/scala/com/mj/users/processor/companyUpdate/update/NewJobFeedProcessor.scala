package com.mj.users.processor.companyUpdate.update

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{Job, JobRequest, Update, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.PostDao.{insertNewJobFeed}
import com.mj.users.mongo.Neo4jConnector.updateNeo4j
import com.mj.users.notification.NotificationRoom
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global




class NewJobFeedProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  

def receive = {

    case (jobRequestDto: JobRequest, notificationRoom: NotificationRoom) => {
      val origin = sender()
      
      val joby = JobRequest(
        jobRequestDto.memberID,
        jobRequestDto.status, jobRequestDto.coyID, jobRequestDto.jobID, jobRequestDto.company_name, 
        jobRequestDto.company_url, jobRequestDto.about_us, jobRequestDto.company_size, jobRequestDto.logo, jobRequestDto.title, 
        jobRequestDto.job_description,jobRequestDto.job_function, 
        jobRequestDto.industry, jobRequestDto.job_location, jobRequestDto.cover_image , jobRequestDto.employment_type , jobRequestDto.level
      )
     
      val result = insertNewJobFeed(joby).flatMap(feedResponse => {
          notificationRoom.notificationActor ! feedResponse
          //println(feedResponse)
          //sendPostToKafka(feedResponse.toJson.toString)
          //println(feedResponse.toJson.toString)
          val script = s"CREATE (s:feeds {memberID:'${jobRequestDto.memberID}', FeedID: '${feedResponse._id}', post_date: TIMESTAMP()})"
          updateNeo4j(script)
          
        }).map(resp => origin ! joby)
            //println("postResponse:"+postResponse.toJson.toString)
          //sendPostToKafka(postResponse.toJson.toString)

      /**
      val result = insertNewJobFeed(jobRequestDto, "Job").map(response => {
        println(response)
       
        origin ! jobRequestDto
      })

      */

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
