package com.mj.users.route.notification

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.careercolony.postservices.notification.NotificationRoom
import org.java_websocket._


trait NotificationService {

  implicit val system: ActorSystem
  implicit val materializer: Materializer

  val notificationRoom: NotificationRoom

  def notification: Route =
    path("notification" / "memberID" / Segment / "count") { memberID =>
      println(s"received $memberID")
      handleWebSocketMessages(notificationRoom.webSocketFlowForCount(memberID))
    } ~ path("notification" / "memberID" / Segment / "posts") { memberID =>
      println(s"received $memberID")
      handleWebSocketMessages(notificationRoom.webSocketFlowForPost(memberID))

    }
}

