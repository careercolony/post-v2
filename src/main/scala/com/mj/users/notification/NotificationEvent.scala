package com.mj.users.notification

import akka.actor.ActorRef
import com.mj.users.model.Feed

object NotificationEvent {

  sealed trait ChatEvent

  case class UserJoinedForCount(id: String, userActor: ActorRef) extends ChatEvent

  case class UserJoinedForNotfyFeedCount(id: String, userActor: ActorRef) extends ChatEvent

  case class UserJoinedForPost(id: String, userActor: ActorRef) extends ChatEvent

  case class UserLeft(id: String) extends ChatEvent

  case class IncomingMessage(text: String) extends ChatEvent

  case class NotificationCount(count: Int) extends ChatEvent {
    override def toString: String = "{\"count\": " + count.toString + "}"
  }

  case class NotificationFeedCount(count: Int) extends ChatEvent {
    override def toString: String = "{\"count\": " + count.toString + "}"
  }

  case class NotificationPost(listOfPost: List[Feed]) extends ChatEvent {
    override def toString: String = if (listOfPost.nonEmpty) "[" + listOfPost.map(_.toString).reduce(_ + "," + _) + "]" else "[]"
  }

  case class ChatMessage(sender: Int, text: String) extends ChatEvent

}
