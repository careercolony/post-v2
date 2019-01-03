package com.careercolony.postservices.notification

import akka.actor.{Actor, ActorRef}
import com.mj.users.mongo.PostDao.{getFriendsUnreadPost,getFriends}

import com.careercolony.postservices.notification.NotificationEvent._
import com.mj.users.model.{Feed, GetFriends}

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class NotificationActor extends Actor  {

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

    case getPost: Feed =>
      getFriends(getPost.memberID).map(friends =>
        friends.foreach(frd => {
          wsCount.get(frd.memberID).foreach(_ ! NotificationCount(1))
          wsPost.get(frd.memberID).foreach(_ ! NotificationPost(List(getPost)))
        })
      )


  }

}
