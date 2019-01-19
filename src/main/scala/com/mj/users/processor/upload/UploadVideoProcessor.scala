package com.mj.users.processor.upload

import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.{Base64, UUID}

import akka.actor.Actor
import akka.http.scaladsl.model.Multipart
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
import com.mj.users.config.MessageConfig
import com.mj.users.model.{UploadImageResponse, responseMessage, uploadVideoResponse}
import com.mj.users.mongo.KafkaAccess
import reactivemongo.bson.{BSONDocument, BSONElement, BSONString}

import scala.concurrent.ExecutionContext.Implicits.global

class UploadVideoProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)

  def getImageName(actualFileName: Option[String]): String = {
    val base64FileName = Base64.getEncoder.encodeToString(UUID.randomUUID().toString.getBytes())
    "D:/" + base64FileName + "." + actualFileName.getOrElse(".jpg").split('.')(1)
  }

  def getFileNameAndTitle(document: BSONDocument): String = {
    val elements: List[BSONElement] = document.elements.toList
    elements.find(_.name == "filename").map(_.value.asInstanceOf[BSONString].value).getOrElse("")
  }

  def receive = {

    case (fileData: Multipart.FormData, materializer: ActorMaterializer) => {
      val origin = sender()
      implicit val systemMaterializer = materializer
      val result = fileData.parts.mapAsync(1) { bodyPart ⇒
        bodyPart.name match {
          case "file" =>
            val fileName = getImageName(bodyPart.filename)
            new java.io.File(fileName).createNewFile()
            val fileOutput: FileOutputStream = new FileOutputStream(fileName)
            bodyPart.entity.dataBytes.runFold(Array.empty[Byte])((array: Array[Byte], byteString: ByteString) => {
              val byteArray: Array[Byte] = byteString.toArray
              fileOutput.write(byteArray)
              array ++ byteArray
            }).map(binaryDAta => {
              fileOutput.close()
              BSONDocument("filename" -> BSONString(fileName)
              )
            })
        }
      }.runFold(
        BSONDocument())((x, y) => {
        x.merge(y)
      })
        .map(bsonData => {
          origin ! uploadVideoResponse(getFileNameAndTitle(bsonData))
        })


      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
