package com.mj.users.route.notification

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.mj.users.notification.NotificationRoom

/*import com.careercolony.postservices.notification.NotificationRoom
import org.java_websocket._*/


trait NotificationService {

  /* implicit val system: ActorSystem
   implicit val materializer: Materializer*/


  def notification(system: ActorSystem, notificationRoom: NotificationRoom): Route =
    path("notification" / "memberID" / Segment / "count") { memberID =>
      //al notificationRoom: NotificationRoom = new NotificationRoom(system)
      println(s"received $memberID")
      handleWebsocketMessages(notificationRoom.webSocketFlowForCount(memberID))

    } ~ path("notification" / "memberID" / Segment / "posts") { memberID =>
      // val notificationRoom: NotificationRoom = new NotificationRoom(system)
      println(s"received $memberID")
      handleWebsocketMessages(notificationRoom.webSocketFlowForPost(memberID))

    }
}

