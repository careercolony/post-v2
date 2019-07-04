package com.mj.users.model


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
case class PostRequest(memberID: String, coyID: Option[String], 
                       title: Option[String], post_body: Option[String], message: Option[String], post_type: Option[String],
                       author: Option[String], author_avatar: Option[String], headline: Option[String],
                       thumbnail_url: Option[List[String]],
                       provider_name: Option[String], provider_url: Option[String],
                       post_url: Option[String], cover_image: Option[String],
                       readers: Option[List[String]])


case class Post(memberID: String, coyID: Option[String], status: String ,postID: String, post_date: String,updated_date : String, title: Option[String],
                post_body: Option[String], message: Option[String], post_type: Option[String],
                author: Option[String], author_avatar: Option[String], headline: Option[String],
                thumbnail_url: Option[List[String]],
                provider_name: Option[String], provider_url: Option[String],
                post_url: Option[String], cover_image: Option[String],
                readers: Option[List[String]], likes: Option[List[LikeDetails]], Shares: Option[List[String]])

*/

case class PostRequest(memberID: String, coyID:Option[String], title: Option[String], 
                       post_body: Option[String], message: Option[String], post_type: Option[String],
                       author: Option[String], author_avatar: Option[String],
                       headline: Option[String], thumbnail_url: Option[List[String]],
                       provider_name: Option[String], provider_url: Option[String],
                       post_url: Option[String], cover_image: Option[String],
                       readers: Option[List[String]])

case class Post(memberID: String, coyID: Option[String], status : String ,postID: String, post_date: String,updated_date : String , title: Option[String],
                post_body: Option[String], message: Option[String], post_type: Option[String],
                author: Option[String], author_avatar: Option[String], headline: Option[String],
                thumbnail_url: Option[List[String]],
                provider_name: Option[String], provider_url: Option[String],
                post_url: Option[String], cover_image: Option[String],
                readers: Option[List[String]], likes: Option[List[LikeDetails]], Shares: Option[List[String]])

case class LikeDetails(likerID: String, liker_headline:Option[String], likerAvatar:Option[String], 
        likerName:Option[String],  like: Option[String], like_date: String)

case class ShareDetails(shareerID: String, shareer_headline:Option[String], shareerAvatar:Option[String], 
        shareerName:Option[String], shareer_date: String)


//case class CommentAction(actorID: String, actorName: Option[String], actorAvatar: Option[String],
                          //actorHeadline:Option[String],
                          //comment_body: Option[String], replies: Option[List[String]])
/**
case class CommentRequest(memberID: String, coyID:Option[String], postID: String,
                          title: Option[String], post_body: Option[String], message: Option[String], post_type: Option[String],
                          author: Option[String], author_avatar: Option[String], headline: Option[String],
                          thumbnail_url: Option[List[String]],
                          provider_name: Option[String], provider_url: Option[String],
                          post_url: Option[String], cover_image: Option[String],
                          readers: Option[List[String]], commentAction: CommentAction)

case class Comment(commentID: String, coyID:Option[String], status : String ,memberID: String, postID: String,
                   actorAvatar: Option[String], headline:Option[String], fullname: Option[String],
                   comment_body: Option[String],
                   comment_date: String,updated_date : String , replies: Option[List[String]], likes: Option[List[String]])

*/

case class CommentRequest(memberID: String, coyID:Option[String], postID: String, comment_text: Option[String],
                          actorID: String, actorName: Option[String], actorAvatar: Option[String], actorHeadline: Option[String],
                          author_avatar: Option[String], headline: Option[String],
                          title: Option[String],
                          post_body: Option[String], message: Option[String], post_type: Option[String],
                          author: Option[String], thumbnail_url: Option[List[String]],
                          provider_name: Option[String], provider_url: Option[String],
                          post_url: Option[String], cover_image: Option[String], activityType: Option[String],
                          readers: Option[List[String]])


case class Comment(commentID: String, status : String ,memberID: String,
                   avatar: Option[String], fullname: Option[String], actorHeadline:Option[String],
                   postID: String, comment_text: Option[String],
                   comment_date: String,updated_date : String, likes: Option[List[String]])

case class LikePostRequest(memberID: String, postID: String, coyID:Option[String], like: Option[String],
                           actorID: String, actorName: Option[String], actorAvatar: Option[String], actorHeadline: Option[String],
                           author_avatar: Option[String], headline: Option[String],
                           title: Option[String],
                           post_body: Option[String], message: Option[String], post_type: Option[String],
                           author: Option[String], thumbnail_url: Option[List[String]],
                           provider_name: Option[String], provider_url: Option[String],
                           post_url: Option[String], cover_image: Option[String], activityType: Option[String],
                           readers: Option[List[String]])


case class LikePostResponse(memberID: String, likerID: String, liker_headline:Option[String], likerAvatar:Option[String], 
        likerName:Option[String], postID: String,  like: Option[String], like_date: String)

case class ReaderFeedRequest(feedID: String, memberID: String)

case class PostShare(memberID: String, postID: String, coyID:Option[String], 
                           actorID: String, actorName: Option[String], actorAvatar: Option[String], actorHeadline: Option[String],
                           author_avatar: Option[String], headline: Option[String],
                           title: Option[String],
                           post_body: Option[String], message: Option[String], post_type: Option[String],
                           author: Option[String], thumbnail_url: Option[List[String]],
                           provider_name: Option[String], provider_url: Option[String],
                           post_url: Option[String], cover_image: Option[String], activityType: Option[String],
                           readers: Option[List[String]], recipients: Option[List[String]])



case class JobRequest(memberID:String, status: String, coyID:String, jobID: String, post_date: String, updated_date : String, company_name: String, 
              company_url: String, about_us:String, company_size:Int, logo: String, title:String, 
              job_description:String, job_function:String, industry:String, 
              job_location:String, cover_image:String, employment_type:String, 
              level: Option[String], views: Option[List[String]])


case class Job(memberID:String, status: String, coyID:String, postID: String, post_date: String, updated_date : String, company_name: String, 
              company_url: String, about_us:String, company_size:Int, logo: String, title:String, 
              job_description:String, job_function: String, industry: String, 
              job_location:String, cover_image: String, employment_type: String, 
              level: Option[String], views: Option[List[String]])

//case class Feed(_id: String, memberID: String, coyID: Option[String], activityType: String, postDetails: Post, actorID: Option[String], actorName: Option[String],actorHeadline:Option[String], actorAvatar: Option[String], commentID: Option[String])
case class Feed(_id: String, memberID: String, coyID:Option[String], activityType: String, postDetails: Post, actorID: Option[String], actorName: Option[String], actorHeadline: Option[String], actorAvatar: Option[String], commentID: Option[String])
case class Activity(_id: String, memberID: String, coyID:Option[String], activityType: String, postDetails: Post, actorID: Option[String], actorName: Option[String], actorHeadline: Option[String], actorAvatar: Option[String], commentID: Option[String])


case class FeedJob(_id: String, memberID: String, activityType: String, postDetails: Job, actorID: Option[String], actorName: Option[String], actorAvatar: Option[String])

//case class FeedUpdate(_id: String, memberID: String, activityType: String, postDetails: Update, actorID: Option[String], actorName: Option[String], actorAvatar: Option[String], commentID: Option[String])

//Response format for all apis
case class responseMessage(uid: String, errmsg: String, successmsg: String)


//Comment like
case class LikeCommentRequest(memberID: String, commentID: String, like: Option[String])

//Reply like
case class LikeReplyRequest(memberID: String, replyID: String, like: Option[String])

case class GetFriends(memberID: String)

case class LikeCommentResponse(memberID: String, commentID: String, like: Option[String], like_date: String)

case class LikeReplyResponse(memberID: String, replyID: String, like: Option[String], like_date: String)

case class ReplyRequest(actorID: String, commentID: String, actorName: String, actorHeadline:Option[String],
                        actorAvatar: Option[String], reply_text: Option[String])


case class Reply(replyID: String, status: String, commentID: String, actorID: String,
                 actorName: String, actorHeadline:Option[String], actorAvatar: Option[String],
                 reply_text: Option[String],
                 reply_date: String)


object JsonRepo extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val PostRequestFormats: RootJsonFormat[PostRequest] = jsonFormat15(PostRequest)
  implicit val likeDetailsFormats: RootJsonFormat[LikeDetails] = jsonFormat6(LikeDetails)
  implicit val shareDetailsFormats: RootJsonFormat[ShareDetails] = jsonFormat5(ShareDetails)

  implicit val PostUpdateRequestFormats: RootJsonFormat[Post] = jsonFormat21(Post)
  implicit val errorMessageDtoFormats: RootJsonFormat[responseMessage] = jsonFormat3(responseMessage)
  //implicit val actionRequestFormats: RootJsonFormat[CommentAction] = jsonFormat6(CommentAction)
  implicit val commentRequestFormats: RootJsonFormat[CommentRequest] = jsonFormat22(CommentRequest)
  implicit val likePostRequestFormats: RootJsonFormat[LikePostRequest] = jsonFormat22(LikePostRequest)
  implicit val likePostResponseFormats: RootJsonFormat[LikePostResponse] = jsonFormat8(LikePostResponse)
  implicit val likeCommentRequestFormats: RootJsonFormat[LikeCommentRequest] = jsonFormat3(LikeCommentRequest)
  implicit val likeReplyRequestFormats: RootJsonFormat[LikeReplyRequest] = jsonFormat3(LikeReplyRequest)
  implicit val likeCommentResponseFormats: RootJsonFormat[LikeCommentResponse] = jsonFormat4(LikeCommentResponse)
  implicit val likeReplyResponseFormats: RootJsonFormat[LikeReplyResponse] = jsonFormat4(LikeReplyResponse)
  implicit val postShareRequestFormats: RootJsonFormat[PostShare] = jsonFormat22(PostShare)
  implicit val commentFormats: RootJsonFormat[Comment] = jsonFormat11(Comment)
  implicit val feedFormats: RootJsonFormat[Feed] = jsonFormat10(Feed)
  implicit val activityFormats: RootJsonFormat[Activity] = jsonFormat10(Activity)
  //implicit val feedUpdateFormats: RootJsonFormat[FeedUpdate] = jsonFormat8(FeedUpdate)

  implicit val jobDtoFormats: RootJsonFormat[Job] = jsonFormat20(Job)
  implicit val jobRequestDtoFormats: RootJsonFormat[JobRequest] = jsonFormat20(JobRequest)
  implicit val feedJobFormats: RootJsonFormat[FeedJob] = jsonFormat7(FeedJob)
  
  implicit val readerFeedRequestFormats: RootJsonFormat[ReaderFeedRequest] = jsonFormat2(ReaderFeedRequest)
  implicit val replyRequestFormats: RootJsonFormat[ReplyRequest] = jsonFormat6(ReplyRequest)
  implicit val replyResponseFormats: RootJsonFormat[Reply] = jsonFormat9(Reply)

}
