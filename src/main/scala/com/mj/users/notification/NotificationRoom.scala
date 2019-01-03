package com.careercolony.postservices.notification

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.{FlowShape, OverflowStrategy}
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._
import com.careercolony.postservices.notification.NotificationEvent._


class NotificationRoom(actorSystem: ActorSystem) {

  val notificationActor: ActorRef = actorSystem.actorOf(Props(classOf[NotificationActor]))

  def webSocketFlowForCount(memberId: String): Flow[Message, Message, _] =
    Flow(Source.actorRef[NotificationCount](bufferSize = 5, OverflowStrategy.fail)) {
      implicit builder =>
        chatSource =>
          val fromWebSocket: FlowShape[Message, IncomingMessage] = builder.add(Flow[Message].collect { case TextMessage.Strict(txt) => IncomingMessage( txt) })
          val backToWebSocket = builder.add(Flow[NotificationCount].map { notification: NotificationCount => TextMessage(notification.toString) })
          val chatActorSink = Sink.actorRef[ChatEvent](notificationActor, UserLeft(memberId))
          val actorAsSource = builder.materializedValue.map(actor => UserJoinedForCount(memberId, actor))
          val merge = builder.add(Merge[ChatEvent](2))
          fromWebSocket ~> merge.in(0)
          actorAsSource ~> merge.in(1)
          merge ~> chatActorSink
          chatSource ~> backToWebSocket
          (fromWebSocket.inlet, backToWebSocket.outlet)
    }

  def webSocketFlowForPost(memberId: String): Flow[Message, Message, _] =
    Flow(Source.actorRef[NotificationPost](bufferSize = 5, OverflowStrategy.fail)) {
      implicit builder =>
        chatSource =>
          val fromWebSocket: FlowShape[Message, IncomingMessage] = builder.add(Flow[Message].collect {case TextMessage.Strict(txt) => IncomingMessage( txt) })
          val backToWebSocket = builder.add(Flow[NotificationPost].map { notification: NotificationPost => TextMessage(notification.toString) })
          val chatActorSink = Sink.actorRef[ChatEvent](notificationActor, UserLeft(memberId))
          val actorAsSource = builder.materializedValue.map(actor => UserJoinedForPost(memberId, actor))
          val merge = builder.add(Merge[ChatEvent](2))
          fromWebSocket ~> merge.in(0)
          actorAsSource ~> merge.in(1)
          merge ~> chatActorSink
          chatSource ~> backToWebSocket
          (fromWebSocket.inlet, backToWebSocket.outlet)
    }

}
