package com.mj.users.notification

import akka.actor.{Actor, ActorRef}
import com.mj.users.model.Feed
import com.mj.users.mongo.PostDao.{getFriends, getFriendsUnreadPost}
import com.mj.users.notification.NotificationEvent._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class NotificationActor extends Actor {

  var wsCount: Map[String, ActorRef] = Map.empty[String, ActorRef]
  var wsPost: Map[String, ActorRef] = Map.empty[String, ActorRef]

  override def receive: Receive = {
    case UserJoinedForCount(id, actorRef) =>
      wsCount += id -> actorRef
      getFriendsUnreadPost(id).onComplete {
        case Success(posts) =>
          actorRef ! NotificationCount(posts.size)
          println(s"Member ID $id connected into web socket!")
        case Failure(error) =>
          println(s"Member ID $id get post failed error: $error")
      }

    case UserJoinedForPost(id, actorRef) =>
      wsPost += id -> actorRef
      getFriendsUnreadPost(id).onComplete {
        case Success(posts) =>
          actorRef ! NotificationPost(posts)
          println(s"Member ID $id connected into web socket!")
        case Failure(error) =>
          println(s"Member ID $id get post failed error: $error")
      }

    case UserLeft(id) =>
      println(s"Member ID $id left connection!")
      wsCount -= id
      wsPost -= id

    case getPost: Feed => {
      getFriends(getPost.memberID).map(friends => {
        friends.foreach(frd => {
          val memberID = frd.memberID.substring(1, frd.memberID.length() - 1)
          wsCount.get(memberID).foreach(_ ! NotificationCount(1))
          wsPost.get(memberID).foreach(_ ! NotificationPost(List(getPost)))
        })
      }
      )
    }


  }

}
