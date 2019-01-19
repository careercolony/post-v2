package com.mj.users.model


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}


case class PostRequest(memberID: String, title: Option[String],
                       description: Option[String], message: Option[String], post_type: Option[String],
                       author: Option[String], author_avatar: Option[String], author_position: Option[String],
                       author_current_employer: Option[String], thumbnail_url: Option[String],
                       provider_name: Option[String], provider_url: Option[String],
                       post_url: Option[String], html: Option[String],
                       readers: Option[List[String]])

case class Post(memberID: String, postID: String, post_date: String, title: Option[String],
                description: Option[String], message: Option[String], post_type: Option[String],
                author: Option[String], author_avatar: Option[String], author_position: Option[String],
                author_current_employer: Option[String], thumbnail_url: Option[String],
                provider_name: Option[String], provider_url: Option[String],
                post_url: Option[String], html: Option[String],
                readers: Option[List[String]], likes: Option[List[LikeDetails]], Shares: Option[List[String]])

case class LikeDetails(likeID: String, like: Option[String], like_date: String)

case class CommentRequest(memberID: String, postID: String, comment_text: Option[String],
                          actorID: String, actorName: Option[String], actorAvatar: Option[String],
                          author_avatar: Option[String], author_position: Option[String],
                          author_current_employer: Option[String], title: Option[String],
                          description: Option[String], message: Option[String], post_type: Option[String],
                          author: Option[String], thumbnail_url: Option[String],
                          provider_name: Option[String], provider_url: Option[String],
                          post_url: Option[String], html: Option[String], activityType: Option[String],
                          replies: Option[List[String]], readers: Option[List[String]])


case class Comment(commentID: String, memberID: String,
                   avatar: Option[String], fullname: Option[String],
                   postID: String, comment_text: Option[String],
                   comment_date: String, replies: Option[List[String]], likes: Option[List[String]])


case class LikePostRequest(memberID: String, postID: String, like: Option[String],
                           actorID: String, actorName: Option[String], actorAvatar: Option[String],
                           author_avatar: Option[String], author_position: Option[String], author_current_employer: Option[String],
                           title: Option[String],
                           description: Option[String], message: Option[String], post_type: Option[String],
                           author: Option[String], thumbnail_url: Option[String],
                           provider_name: Option[String], provider_url: Option[String],
                           post_url: Option[String], html: Option[String], activityType: Option[String],
                           readers: Option[List[String]])


case class LikePostResponse(memberID: String, likeID: String, postID: String, like: Option[String], like_date: String)

case class ReaderFeedRequest(feedID: String, memberID: String)

case class PostShare(memberID: String, postID: String, recipients: Option[List[String]])


case class Feed(_id: String, memberID: String, activityType: String, postDetails: Post, actorID: Option[String], actorName: Option[String], actorAvatar: Option[String], commentID: Option[String])

//Response format for all apis
case class responseMessage(uid: String, errmsg: String, successmsg: String)


//Comment like
case class LikeCommentRequest(memberID: String, commentID: String, like: Option[String])

case class GetFriends(memberID: String)

case class LikeCommentResponse(memberID: String, commentID: String, like: Option[String], like_date: String)

case class ReplyRequest(actorID: String, commentID: String, actorName: String,
                        actorAvatar: Option[String], reply_text: Option[String])


case class Reply(replyID: String, commentID: String, actorID: String,
                 actorName: String, actorAvatar: Option[String],
                 reply_text: Option[String],
                 reply_date: String)

case class UploadImageResponse ( fileName  : String )

object JsonRepo extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val PostRequestFormats: RootJsonFormat[PostRequest] = jsonFormat15(PostRequest)
  implicit val likeDetailsFormats: RootJsonFormat[LikeDetails] = jsonFormat3(LikeDetails)

  implicit val PostUpdateRequestFormats: RootJsonFormat[Post] = jsonFormat19(Post)
  implicit val errorMessageDtoFormats: RootJsonFormat[responseMessage] = jsonFormat3(responseMessage)
  implicit val commentRequestFormats: RootJsonFormat[CommentRequest] = jsonFormat22(CommentRequest)
  implicit val likePostRequestFormats: RootJsonFormat[LikePostRequest] = jsonFormat21(LikePostRequest)
  implicit val likePostResponseFormats: RootJsonFormat[LikePostResponse] = jsonFormat5(LikePostResponse)
  implicit val likeCommentRequestFormats: RootJsonFormat[LikeCommentRequest] = jsonFormat3(LikeCommentRequest)
  implicit val likeCommentResponseFormats: RootJsonFormat[LikeCommentResponse] = jsonFormat4(LikeCommentResponse)
  implicit val postShareRequestFormats: RootJsonFormat[PostShare] = jsonFormat3(PostShare)
  implicit val commentFormats: RootJsonFormat[Comment] = jsonFormat9(Comment)
  implicit val feedFormats: RootJsonFormat[Feed] = jsonFormat8(Feed)
  implicit val readerFeedRequestFormats: RootJsonFormat[ReaderFeedRequest] = jsonFormat2(ReaderFeedRequest)
  implicit val replyRequestFormats: RootJsonFormat[ReplyRequest] = jsonFormat5(ReplyRequest)
  implicit val replyResponseFormats: RootJsonFormat[Reply] = jsonFormat7(Reply)
  implicit val uploadImageResponseFormats: RootJsonFormat[UploadImageResponse] = jsonFormat1(UploadImageResponse)
}
