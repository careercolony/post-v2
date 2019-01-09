package com.mj.users.processor.post

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.directive.PageRequest
import com.mj.users.model.{Feed, GetFriends, responseMessage}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.mongo.Neo4jConnector.getNeo4j
import com.mj.users.mongo.PostDao.retrieveFriendsPosts

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetFriendsPostProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (memberID: String, pageOpt: Option[PageRequest]) => {
      val origin = sender()
      val script: String = s"MATCH (me { memberID: '$memberID' })-[rels:FRIEND*1..3]-(myfriend) WHERE ALL (r IN rels WHERE r.status = 'active') WITH COLLECT(myfriend) AS collected_friends UNWIND collected_friends AS activity  MATCH (p:feeds {memberID:activity.memberID}) WITH (p) AS e return e.memberID AS memberID"
      val result = getNeo4j(script).flatMap(result => {
        var records: List[GetFriends] = List[GetFriends]()
        while (result.hasNext) {
          val record = result.next()
          records :+= GetFriends(record.get("memberID").toString)
        }
        Future {
          records.distinct
        }
      }).map(response =>{
        println("memberID:"+response)
        retrieveFriendsPosts(response.map(_.memberID), pageOpt).mapTo[List[Feed]].map(_.filterNot(_.postDetails.readers.exists(_.contains(memberID.toString))))
          .map(/*_.filterNot(_.postDetails.readers.exists(_.contains(memberID.toString))*/
          response =>{
            println("resp"+response)
            origin ! response})}
      )


      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
