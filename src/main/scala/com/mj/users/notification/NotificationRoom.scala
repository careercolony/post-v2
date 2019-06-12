
package com.mj.users.notification

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{FlowGraph, _}
import akka.stream.{FlowShape, OverflowStrategy}
import com.mj.users.notification.NotificationEvent._


class NotificationRoom(actorSystem: ActorSystem) {

  val notificationActor: ActorRef = actorSystem.actorOf(Props(classOf[NotificationActor]))


  def webSocketFlowForCount(memberId: String): Flow[Message, Message, _] =
    Flow.fromGraph(

      FlowGraph.create(Source.actorRef[NotificationCount](bufferSize = 5, OverflowStrategy.fail)) {
        implicit builder =>
          chatSource =>

            import FlowGraph.Implicits._

            //input flow, all Messages
            val fromWebsocket = builder.add(
              Flow[Message].collect {
                case TextMessage.Strict(txt) => IncomingMessage(txt)
              })

            //          //output flow, it returns Message's
            val backToWebsocket = builder.add(
              Flow[NotificationCount].map {
                case notification: NotificationCount => TextMessage(notification.count.toString)
              }
            )

            //          //send messages to the actor, if send also UserLeft(user) before stream completes.
            val chatActorSink = Sink.actorRef[ChatEvent](notificationActor, UserLeft(memberId))

            //          //merges both pipes
            val merge = builder.add(Merge[ChatEvent](2))

            //          //Materialized value of Actor who sit in chatroom
            val actorAsSource = builder.materializedValue.map(actor => UserJoinedForCount(memberId, actor))

            //          //Message from websocket is converted into IncommingMessage and should be send to each in room
            fromWebsocket ~> merge.in(0)

            //          //If Source actor is just created should be send as UserJoined and registered as particiant in room
            actorAsSource ~> merge.in(1)

            //          //Merges both pipes above and forward messages to chatroom Represented by ChatRoomActor
            merge ~> chatActorSink

            //          //Actor already sit in chatRoom so each message from room is used as source and pushed back into websocket
            chatSource ~> backToWebsocket

            //          // expose ports
            FlowShape(fromWebsocket.inlet, backToWebsocket.outlet)
      }

    )


  def webSocketFlowForPost(memberId: String): Flow[Message, Message, _] =
    Flow.fromGraph(

      FlowGraph.create(Source.actorRef[NotificationPost](bufferSize = 5, OverflowStrategy.fail)) {
        implicit builder =>
          chatSource =>

            import FlowGraph.Implicits._

            //input flow, all Messages
            val fromWebsocket = builder.add(
              Flow[Message].collect {
                case TextMessage.Strict(txt) => IncomingMessage(txt)
              })

            //          //output flow, it returns Message's
            val backToWebsocket = builder.add(
              Flow[NotificationPost].map {
                case notification: NotificationPost => {
                  println("notification:" + notification)
                  TextMessage(notification.toString)
                }
              }
            )

            //          //send messages to the actor, if send also UserLeft(user) before stream completes.
            val chatActorSink = Sink.actorRef[ChatEvent](notificationActor, UserLeft(memberId))

            //          //merges both pipes
            val merge = builder.add(Merge[ChatEvent](2))

            //          //Materialized value of Actor who sit in chatroom
            val actorAsSource = builder.materializedValue.map(actor => UserJoinedForPost(memberId, actor))

            //          //Message from websocket is converted into IncommingMessage and should be send to each in room
            fromWebsocket ~> merge.in(0)

            //          //If Source actor is just created should be send as UserJoined and registered as particiant in room
            actorAsSource ~> merge.in(1)

            //          //Merges both pipes above and forward messages to chatroom Represented by ChatRoomActor
            merge ~> chatActorSink

            //          //Actor already sit in chatRoom so each message from room is used as source and pushed back into websocket
            chatSource ~> backToWebsocket

            //          // expose ports
            FlowShape(fromWebsocket.inlet, backToWebsocket.outlet)
      }

    )

  def webSocketFlowForNofiyFeedCount(memberId: String): Flow[Message, Message, _] =
    Flow.fromGraph(

      FlowGraph.create(Source.actorRef[NotificationFeedCount](bufferSize = 5, OverflowStrategy.fail)) {
        implicit builder =>
          chatSource =>

            import FlowGraph.Implicits._

            //input flow, all Messages
            val fromWebsocket = builder.add(
              Flow[Message].collect {
                case TextMessage.Strict(txt) => IncomingMessage(txt)
              })

            //          //output flow, it returns Message's
            val backToWebsocket = builder.add(
              Flow[NotificationFeedCount].map {
                case notification: NotificationFeedCount => TextMessage(notification.count.toString)
              }
            )

            //          //send messages to the actor, if send also UserLeft(user) before stream completes.
            val chatActorSink = Sink.actorRef[ChatEvent](notificationActor, UserLeft(memberId))

            //          //merges both pipes
            val merge = builder.add(Merge[ChatEvent](2))

            //          //Materialized value of Actor who sit in chatroom
            val actorAsSource = builder.materializedValue.map(actor => UserJoinedForNotfyFeedCount(memberId, actor))

            //          //Message from websocket is converted into IncommingMessage and should be send to each in room
            fromWebsocket ~> merge.in(0)

            //          //If Source actor is just created should be send as UserJoined and registered as particiant in room
            actorAsSource ~> merge.in(1)

            //          //Merges both pipes above and forward messages to chatroom Represented by ChatRoomActor
            merge ~> chatActorSink

            //          //Actor already sit in chatRoom so each message from room is used as source and pushed back into websocket
            chatSource ~> backToWebsocket

            //          // expose ports
            FlowShape(fromWebsocket.inlet, backToWebsocket.outlet)
      }

    )

}


