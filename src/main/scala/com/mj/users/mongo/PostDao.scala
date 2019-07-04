package com.mj.users.mongo

import java.text.SimpleDateFormat

import com.mj.users.directive.Order.{Asc, Desc}
import com.mj.users.directive.PageRequest
import com.mj.users.model._
import com.mj.users.mongo.MongoConnector._
import com.mj.users.mongo.Neo4jConnector.getNeo4j
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import reactivemongo.api.QueryOpts
import org.joda.time.DateTime
import com.mj.users.config.Application._
import scala.concurrent.Future

object PostDao {

  val format: SimpleDateFormat = new SimpleDateFormat("yyyy MM dd HH:mm:ss")
  val postCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("post"))
  val replytCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("reply"))
  val feedCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("feed"))
  val commentCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("comment"))
  val updateCollection: Future[BSONCollection] = dbcoy.map(_.collection[BSONCollection]("updates"))
  

  implicit def replyWriter = Macros.handler[Reply]

  implicit def PostRequestWriter = Macros.handler[PostRequest]

  implicit def likeDetailsWriter = Macros.handler[LikeDetails]
  implicit def shareDetailsWriter = Macros.handler[ShareDetails]

  implicit def postWriter = Macros.handler[Post]
 
  implicit def jobWriter = Macros.handler[Job]

  implicit def feedWriter = Macros.handler[Feed]
  implicit def feedJobWriter = Macros.handler[FeedJob]
  

  implicit def commentWriter = Macros.handler[Comment]

  //Get all Feed
  def getFeedStore(): Future[List[Feed]] = {

    val result = searchAll[Feed](feedCollection,
      BSONDocument("activityType" -> "Post"))
    println("result:" + result)
    result
  }

  //Get All Post
  def getMemberIDStore(memberID: String, page: PageRequest): Future[List[Post]] = {
    val sort: BSONDocument = getPaginationSort(page)
    val queryOps = QueryOpts(skipN = page.offset, batchSizeN = page.limit, flagsN = 0)
    searchWithPagination[Post](postCollection,
      BSONDocument("memberID" -> memberID, "status" -> active), queryOps, sort, page.limit)
  }


  def getFriends(memberID: String): Future[List[GetFriends]] = {
    val script: String = s"MATCH (me { memberID: '$memberID' })-[rels:FRIEND*1..3]-(myfriend) WHERE ALL (r IN rels WHERE r.status = 'active') WITH COLLECT(myfriend) AS collected_friends UNWIND collected_friends AS activity  MATCH (p:feeds {memberID:activity.memberID}) WITH (p) AS e return e.memberID AS memberID"
    val result = getNeo4j(script).flatMap(result => {
      var records: List[GetFriends] = List[GetFriends]()
      while (result.hasNext) {
        val record = result.next()
        records :+= GetFriends(record.get("memberID").toString)
      }
      Future {
        records.distinct
      }
    })

    result
  }

  def getFriendsUnreadPost(memberID: String): Future[List[Feed]] = {

    getFriends(memberID).flatMap(resp =>
      retrieveFriendsNotifications(resp.map(_.memberID), None).mapTo[List[Feed]]
    ).map(_.filterNot(_.postDetails.readers.exists(_.contains(memberID.toString)))) /*_.filterNot(_.postDetails.readers.exists(_.contains(memberID.toString))*/

  }

  

  def retrieveFriendsNotifications(listOfMemberId: List[String], pageOpt: Option[PageRequest]): Future[List[Feed]] = {
    val page: PageRequest = pageOpt.getOrElse(PageRequest.default.copy(sort = Map("postID" -> Desc)))
    val sort: BSONDocument = getPaginationSort(page)
    val queryOps = QueryOpts(skipN = page.offset, batchSizeN = 1000, flagsN = 0)
    val memberList = listOfMemberId.map(element => element.toString.substring(1, element.toString.length() - 1))
    searchWithPagination[Feed](feedCollection,
      BSONDocument("memberID" -> BSONDocument("$in" -> memberList)), queryOps, sort, 1000)
  }

  def retrieveFriendsPosts(listOfMemberId: List[String], pageOpt: Option[PageRequest]): Future[List[Feed]] = {
    val page: PageRequest = pageOpt.getOrElse(PageRequest.default.copy(sort = Map("postID" -> Desc)))
    val sort: BSONDocument = getPaginationSort(page)
    val queryOps = QueryOpts(skipN = page.offset, batchSizeN = page.limit, flagsN = 0)
    val memberList = listOfMemberId.map(element => element.toString.substring(1, element.toString.length() - 1))
    searchWithPagination[Feed](feedCollection,
      BSONDocument("memberID" -> BSONDocument("$in" -> memberList), BSONDocument("activityType" -> BSONDocument("$in" -> BSONArray("Post", "Comment", "liked", "Job"))) ), queryOps, sort, page.limit)
  }

  private def getPaginationSort(page: PageRequest) = {
    val sort: BSONDocument = BSONDocument(page.sort.map {
      case (k, Asc) => (k, BSONInteger(1))
      case (k, Desc) => (k, BSONInteger(-1))
    })
    sort
  }

  //insert user Details
  def insertNewPost(userRequest: PostRequest): Future[Post] = {

    for {
      postData <- Future {
        Post(userRequest.memberID,userRequest.coyID,active,
          BSONObjectID.generate().stringify,
          DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
          userRequest.title, userRequest.post_body, userRequest.message, userRequest.post_type,
          userRequest.author, userRequest.author_avatar, userRequest.headline,
          userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
          userRequest.post_url, userRequest.cover_image, userRequest.readers, None,
          None
        )
      }
      response <- insert[Post](postCollection, postData)
    }
      yield (response)
  }

  //update user Details
  def updateNewPost(userRequest: Post): Future[String] = {
    for {

      response <- updateDetails[Post](postCollection, BSONDocument("postID" -> userRequest.postID), userRequest.copy(updated_date =  DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")))
    }
      yield (response)
  }


  


  def getJobFromCompany(jobRequestDto: JobRequest): Future[Job] = {
    for {
      jobData <- Future {
        Job(
          jobRequestDto.memberID, 
          jobRequestDto.status, jobRequestDto.coyID, jobRequestDto.jobID, jobRequestDto.post_date,jobRequestDto.updated_date,
          jobRequestDto.company_name, jobRequestDto.company_url, jobRequestDto.about_us, jobRequestDto.company_size, 
          jobRequestDto.logo, jobRequestDto.title, jobRequestDto.job_description,jobRequestDto.job_function, 
          jobRequestDto.industry, jobRequestDto.job_location, jobRequestDto.cover_image, jobRequestDto.employment_type, jobRequestDto.level, None
        )
      }
    
    }
      yield(jobData)
  }

  

  def insertNewJobFeed(userRequest: Job, feedType: String): Future[FeedJob] = {
    
    for {
      feedData <- Future {
        FeedJob(BSONObjectID.generate().stringify, userRequest.memberID,
          feedType,
          Job(userRequest.memberID,active,
            userRequest.coyID, userRequest.postID,
            DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
            userRequest.company_name, userRequest.company_url, userRequest.about_us, userRequest.company_size, 
            userRequest.logo, userRequest.title, userRequest.job_description, userRequest.job_function, 
            userRequest.industry, userRequest.job_location, userRequest.cover_image,
            userRequest.employment_type, userRequest.level, None
          ),
          None, None, None
        )
      }
      response <- insert[FeedJob](feedCollection, feedData)
    }
      yield (response)
  }
  
  
  

  //Get all like for Post
  def getAllLikesStore(postID: String): Future[List[Post]] = {
    searchAll[Post](postCollection,
      BSONDocument("postID" -> postID, "status" -> active))
  }

  //LikePost
  def LikePost(userRequest: LikePostRequest): Future[String] = {
    for {

      response <- update(postCollection, BSONDocument("postID" -> userRequest.postID, "status" -> active), 
        BSONDocument("$addToSet" -> BSONDocument("likes" -> 
        LikeDetails(userRequest.actorID, userRequest.actorHeadline, userRequest.actorAvatar, 
        userRequest.actorName,   userRequest.like,
        DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")))))
    }
      yield (response)
  }

  // Increase post like counts by 1
  def incrementLikeCount(like: LikePostRequest) = {

    val selector = BSONDocument("postID" -> like.postID)
    val result = for {

      response <- update(postCollection, selector, BSONDocument("$inc" -> BSONDocument("count.like" -> 1)))
    }
    yield("")
  }



  //UnLikePost
  def UnLikePost(postID: String, memberID: String): Future[String] = {
    for {

      response <- update(postCollection, BSONDocument("postID" -> postID, "status" -> active), BSONDocument("$pull" -> BSONDocument("likes" -> BSONDocument("likerID" -> memberID))))
    }
      yield (response)
  }
// decrease post like counts by 1
  def decrementCommentCount(postID: String) = {

    val selector = BSONDocument("postID" -> postID)
    val result = for {

      response <- update(postCollection, selector, BSONDocument("$inc" -> BSONDocument("count.like" -> -1)))
    }
    yield("")
  }

  def updateNewSharePost(userRequest: PostShare): Future[String] = {
    for {

      //response <- update(postCollection, BSONDocument("postID" -> userRequest.postID, "status" -> active
      //), BSONDocument("$addToSet" -> BSONDocument("shares" -> BSONDocument("postID" -> userRequest.postID, "actorID" -> userRequest.actorID, "recipients" -> userRequest.recipients))))
      response <- update(postCollection, BSONDocument("postID" -> userRequest.postID, "status" -> active), 
        BSONDocument("$addToSet" -> BSONDocument("shares" -> 
        ShareDetails(userRequest.actorID, userRequest.actorHeadline, userRequest.actorAvatar, 
        userRequest.actorName,
        DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")))))
    }
      yield (response)
  }



  //insert comment
  def insertNewComment(userRequest: CommentRequest): Future[Comment] = {
    for {
      postData <- Future {
        Comment(BSONObjectID.generate().stringify,
          active,
          userRequest.memberID,
          userRequest.actorAvatar,
          userRequest.actorName,
          userRequest.actorHeadline,
          userRequest.postID,
          userRequest.comment_text,
          DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),
          "",
          None
        )
      }
      response <- insert[Comment](commentCollection, postData)
    }
      yield (response)
  }

  def incrementCommentCount(commentResp: Comment) = {

    val selector = BSONDocument("postID" -> commentResp.postID)
    val result = for {

      response <- update(postCollection, selector, BSONDocument("$inc" -> BSONDocument("count.comment" -> 1)))
    }
    yield("")
  }



  //Get Comment
  def getComment(postID: String): Future[List[Comment]] = {
    searchAll[Comment](commentCollection,
      document("postID" -> postID, "status" -> active))
  }

  //Get Activities
  def getActivities(memberID: String): Future[List[Feed]] = {
    searchAll[Feed](feedCollection,
      document("memberID" -> memberID))
  }



  def getCommentCount(postID: String): Future[List[Comment]] = {
    searchAll[Comment](commentCollection,
      document("postID" -> postID, "status" -> active))
  }

  

  


  //update user Details
  def updateNewFeed(userRequest: Post, feedType: String): Future[String] = {

    val selector = BSONDocument("postDetails.postID" -> userRequest.postID, "activityType" -> "Post")
    val result = for {

      response <- update(feedCollection, selector, BSONDocument(
        "$set" -> BSONDocument("postDetails" -> userRequest)
      ))
    }
      yield (response)

    result.recover {
      case e: Throwable => {
        println("msg:" + e.getMessage)
        throw new Exception(e.getMessage)
      }


    }
  }

  def updateReaderFeed(req: ReaderFeedRequest): Future[String] = {

    val selector = BSONDocument("_id" -> req.feedID)
    val result = for {

      response <- update(feedCollection, selector,
        BSONDocument("$addToSet" -> BSONDocument("postDetails.readers" -> req.memberID)))
    }
      yield (response)

    result.recover {
      case e: Throwable => {
        println("msg:" + e.getMessage)
        throw new Exception(e.getMessage)
      }


    }
  }

  def insertNewPostFeed(userRequest: Post, feedType: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,userRequest.coyID,
          feedType,
          Post(userRequest.memberID,None,active,
            userRequest.postID,
            DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
            userRequest.title, userRequest.post_body, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.headline,
            userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.post_url, userRequest.cover_image, userRequest.readers, None,
            None
          ),
          None, None, None, None, None
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)


  }


  def insertNewCommentFeed(userRequest: CommentRequest, feedType: String, commentResp: Comment): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.actorID,userRequest.coyID,
          feedType,
          Post(userRequest.memberID,None,active,
            userRequest.postID,
            DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
            userRequest.title, userRequest.post_body, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.headline,
            userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.post_url, userRequest.cover_image, userRequest.readers, None,
            None
          ),
          Some(userRequest.actorID),
          userRequest.actorName,
          userRequest.actorHeadline,
          userRequest.actorAvatar, 
          Some(commentResp.commentID)
        )
      }
      response <- insert[Feed](feedCollection, feedData)
      
    }
      yield (response)
      

  }

  
  


/**
  def getPostByID(postID: String): Future[Post] = {
    search[Post](postCollection, document("postID" -> postID))
  }  

  
  def insertNewCommentFeed(userRequest: CommentRequest, feedType: String, commentResp: Comment): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,userRequest.coyID,
          feedType,
          getPostByID(commentResp.postID).mapTo[Post],
          Some(userRequest.actorID),
          userRequest.actorName,
          userRequest.actorAvatar, Some(commentResp.commentID)
        )
      }
      response <- insert[Feed](feedCollection, feedData)
      
    }
      yield (response)


  }
*/

  def insertNewLikeFeed(userRequest: LikePostRequest, feedType: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.actorID,userRequest.coyID,
          feedType,
          Post(userRequest.memberID,None,active,
            userRequest.postID,
            DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
            userRequest.title, userRequest.post_body, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.headline,
            userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.post_url, userRequest.cover_image, userRequest.readers, None,
            None
          ),
          Some(userRequest.actorID),
          userRequest.actorName,
          userRequest.actorHeadline,
          userRequest.actorAvatar, None
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)


  }

  

  def insertNewShareFeed(userRequest: PostShare, feedType: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.actorID,userRequest.coyID,
          feedType,
          Post(userRequest.memberID,None,active,
            userRequest.postID,
            DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
            userRequest.title, userRequest.post_body, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.headline,
            userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.post_url, userRequest.cover_image, userRequest.readers, None,
            None
          ),
          Some(userRequest.actorID),
          userRequest.actorName,
          userRequest.actorHeadline,
          userRequest.actorAvatar, None
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)
  }

  def insertLikeFeedForReply(userRequest: Feed, feedType: String, memberID: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,userRequest.coyID,
          feedType,
          userRequest.postDetails,
          userRequest.actorID,
          userRequest.actorName,
          userRequest.actorHeadline,
          userRequest.actorAvatar,
          userRequest.commentID
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)


  }

/** Comment Like */
  
  //like Comment
  def LikeComment(userRequest: LikeCommentRequest): Future[String] = {
    for {
      response <- update(commentCollection, BSONDocument("commentID" -> userRequest.commentID, "status" -> active), BSONDocument("$addToSet" -> BSONDocument("likes" -> userRequest.memberID)))
    }
    yield (response)
  }

  // Get the comment feed by commentID 
  def getFeedForComment(userRequest: LikeCommentRequest): Future[Option[Feed]] = {
    search[Feed](feedCollection,
      document("commentID" -> userRequest.commentID, "activityType" -> "Comment"))
  }

  // Insert comment_like feed 
  def insertLikeFeedForComment(userRequest: Feed, feedType: String, memberID: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,userRequest.coyID,
          feedType,
          userRequest.postDetails,
          userRequest.actorID,
          userRequest.actorName,
          userRequest.actorHeadline,
          userRequest.actorAvatar,
          userRequest.commentID
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)
  }


  // Increment comment like count
  def incrementLikeCommentCount(commentID: String) = {
    println("okays"+commentID)
    val selector = BSONDocument("commentID" -> commentID)
    val result = for {
      response <- update(commentCollection, selector, BSONDocument("$inc" -> BSONDocument("count.like" -> 1)))
    }
    yield("")
  }

  
  /** Reply like */

  // Increment reply like count
  def incrementLikeReplyCount(replyID: String) = {
    val selector = BSONDocument("replyID" -> replyID)
    val result = for {

      response <- update(replytCollection, selector, BSONDocument("$inc" -> BSONDocument("count.like" -> 1)))
    }
    yield("")
  }

  //Unlike Comment
  def UnLikeComment(commentID: String, memberID: String): Future[String] = {
    for {
      response <- update(commentCollection, BSONDocument("commentID" -> commentID, "status" -> active), BSONDocument("$pull" -> BSONDocument("likes" -> memberID)))
    }
      yield (response)
  }

  // Decrement like comment count
  def decrementLikeCommentCount(commentID: String) = {
    val selector = BSONDocument("commentID" -> commentID)
    val result = for {

      response <- update(commentCollection, selector, BSONDocument("$inc" -> BSONDocument("count.like" -> -1)))
    }
    yield("")
  }

  
  

  /** Comment reply */
  
  // New comment reply 

  def insertNewReply(userRequest: ReplyRequest): Future[Reply] = {

    for {
      replyData <- Future {
        Reply(BSONObjectID.generate().stringify,
          active,
          userRequest.commentID,
          userRequest.actorID,
          userRequest.actorName,
          userRequest.actorHeadline,
          userRequest.actorAvatar, userRequest.reply_text,
          DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")
        )
      }
      response <- insert[Reply](replytCollection, replyData)
    }
      yield (response)
  }

  // Get the comment feed by commentID 
  def getFeedForCommentReply(userRequest: ReplyRequest): Future[Option[Feed]] = {
    search[Feed](feedCollection,
      document("commentID" -> userRequest.commentID, "activityType" -> "Comment"))
  }

  // Create a feed reply
   def insertReplyFeedForComment(userRequest: Feed, feedType: String, memberID: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,userRequest.coyID,
          feedType,
          userRequest.postDetails,
          userRequest.actorID,
          userRequest.actorName,
          userRequest.actorHeadline,
          userRequest.actorAvatar, 
          userRequest.commentID
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)
  }

  // Count comment replies
  def incrementReplyCount(replyResp: ReplyRequest) = {

    val selector = BSONDocument("commentID" -> replyResp.commentID)
    val result = for {
      response <- update(commentCollection, selector, BSONDocument("$inc" -> BSONDocument("count.reply" -> 1)))
    }
    yield("")
  }

  //like reply
  def LikeReply(userRequest: LikeReplyRequest): Future[String] = {
    for {
      response <- update(replytCollection, BSONDocument("replyID" -> userRequest.replyID, "status" -> active), BSONDocument("$addToSet" -> BSONDocument("likes" -> userRequest.memberID)))
    }
      yield (response)
  }

  // Get feed for like reply
  /**
  def getFeedForLikeReply(userRequest: LikeReplyRequest): Future[Option[Feed]] = {
    search[Feed](feedCollection,
      document("activityType" -> "reply_comment", "replyID"-> userRequest.replyID))
  }
  */

  //Unlike Reply
  def UnLikeReply(replyID: String, memberID: String): Future[String] = {
    for {
      response <- update(replytCollection, BSONDocument("replyID" -> replyID, "status" -> active), BSONDocument("$pull" -> BSONDocument("likes" -> memberID)))
    }
      yield (response)
  }
  
  // Decrement r like count
  def decrementReplyLikeCount(replyID: String) = {
    val selector = BSONDocument("replyID" -> replyID)
    val result = for {

      response <- update(replytCollection, selector, BSONDocument("$inc" -> BSONDocument("count.like" -> -1)))
    }
    yield("")
  }


  def getreply(commentID: String): Future[List[Reply]] = {
    searchAll[Reply](replytCollection,
      document("commentID" -> commentID, "status"-> active))
  }


}


