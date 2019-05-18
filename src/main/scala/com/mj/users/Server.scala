package com.mj.users

import java.net.InetAddress

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.mj.users.config.Application
import com.mj.users.config.Application._
import com.mj.users.notification.NotificationRoom
import com.mj.users.tools.CommonUtils._
import com.mj.users.tools.RouteUtils
import com.typesafe.config.ConfigFactory

object Server extends App {
  val seedNodesStr = seedNodes
    .split(",")
    .map(s => s""" "akka.tcp://users-cluster@$s" """)
    .mkString(",")

  val inetAddress = InetAddress.getLocalHost
  var configCluster = Application.config.withFallback(
    ConfigFactory.parseString(s"akka.cluster.seed-nodes=[$seedNodesStr]"))

  configCluster = configCluster
    .withFallback(
      ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostName"))
    .withFallback(
      ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$akkaPort"))

  implicit val system: ActorSystem = ActorSystem("users-cluster", configCluster)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  //post
  val newPostProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.post.NewPostProcessor]), "newPostProcessor")
  val updatePostProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.post.UpdatePostProcessor]), "updatePostProcessor")
  val likePostProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.like.LikePostProcessor]), "likePostProcessor")
  val unLikePostProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.like.UnlikPostProcessor]), "unLikePostProcessor")
  val deletePostProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.post.DeletePostProcessor]), "deletePostProcessor")


  val newCommentProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.comment.NewCommentProcessor]), "newCommentProcessor")
  val getCommentProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.comment.GetCommentProcessor]), "getCommentProcessor")
  val likeCommentProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.like.LikeCommentProcessor]), "likeCommentProcessor")
  val unLikeCommentProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.like.UnlikeCommentProcessor]), "unLikeCommentProcessor")
  val getCommentCountProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.comment.GetCommentCountProcessor]), "getCommentCountProcessor")
  val deleteCommentProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.comment.DeleteCommentProcessor]), "deleteCommentProcessor")


  val sharePostProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.post.SharePostProcessor]), "sharePostProcessor")
  val getFriendsPostProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.post.GetFriendsPostProcessor]), "getFriendsPostProcessor")

  val updateReaderProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.post.UpdateReaderProcessor]), "updateReaderProcessor")

  val newReplyProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.reply.NewReplyProcessor]), "newReplyProcessor")
  val getRepliesProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.reply.GetRepliesProcessor]), "getRepliesProcessor")

  // Update
  val newUpdateProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.companyUpdate.update.NewUpdateProcessor]), "newUpdateProcessor")
  val editUpdateProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.companyUpdate.update.EditUpdateProcessor]), "editUpdateProcessor")
  val GetOneUpdateProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.companyUpdate.update.GetOneUpdateProcessor]), "getOneUpdateProcessor")
  val GetUpdateByMemberProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.companyUpdate.update.GetUpdateByMemberProcessor]), "getUpdateByMemberProcessor")
  val DeleteUpdateProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.companyUpdate.update.DeleteUpdateProcessor]), "deleteUpdateProcessor")
  val newJobFeedProcessor = system.actorOf(RoundRobinPool(poolSize).props(Props[processor.companyUpdate.NewJobFeedProcessor]), "newJobFeedProcessor")
  


  val notificationRoom: NotificationRoom = new NotificationRoom(system)
  import system.dispatcher

  Http().bindAndHandle(RouteUtils.logRoute(notificationRoom,system,materializer), "0.0.0.0", port)

  consoleLog("INFO",
    s"User server started! Access url: https://$hostName:$port/")
}
