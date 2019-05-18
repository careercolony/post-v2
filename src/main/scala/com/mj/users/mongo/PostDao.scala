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

  implicit def postWriter = Macros.handler[Post]
  implicit def updateWriter = Macros.handler[Update]
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
      BSONDocument("memberID" -> BSONDocument("$in" -> memberList), "activityType" -> "Post"), queryOps, sort, page.limit)
  }

  //insert user Details
  def insertNewPost(userRequest: PostRequest): Future[Post] = {

    for {
      postData <- Future {
        Post(userRequest.memberID,active,
          BSONObjectID.generate().stringify,
          DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
          userRequest.title, userRequest.description, userRequest.message, userRequest.post_type,
          userRequest.author, userRequest.author_avatar, userRequest.author_position,
          userRequest.author_current_employer, userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
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

  //insert user Details
  def insertNewUpdate(userRequest: UpdateRequest): Future[Update] = {
    for {
      updateData <- Future {
        Update(userRequest.memberID,
          userRequest.coyID,
          active,
          BSONObjectID.generate().stringify,
          DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
          userRequest.title,
          userRequest.description,
          userRequest.message,
          userRequest.post_type,
          userRequest.author,
          userRequest.author_avatar,
          userRequest.author_position,
          userRequest.author_current_employer,
          userRequest.thumbnail_url,
          userRequest.provider_name,
          userRequest.provider_url,
          userRequest.update_url,
          userRequest.cover_image,
          userRequest.readers,
          None,
          None
        )
      }
      response <- insert[Update](updateCollection, updateData)
    }
      yield (response)
  }

  //update user Details
  def editUpdate(userRequest: Update): Future[String] = {
    for {

      response <- updateDetails[Update](updateCollection, BSONDocument("postID" -> userRequest.postID), userRequest.copy(updated_date =  DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")))
    }
      yield (response)
  }

  def insertNewUpdateFeed(userRequest: Update, feedType: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,
          feedType,
          Post(userRequest.memberID,active,
            userRequest.postID,
            DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
            userRequest.title, userRequest.description, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.author_position,
            userRequest.author_current_employer, userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.update_url, userRequest.cover_image, userRequest.readers, None,
            None
          ),
          None, None, None, None
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)


  }

/**
  def insertNewJob(userRequest: JobRequest): Future[Job] = {
    for {
      jobData <- Future {
        Job(userRequest.memberID,
          userRequest.status, userRequest.coyID,userRequest.jobID,
          userRequest.company_name,
          userRequest.company_url,
          userRequest.about_us,
          userRequest.company_size,
          userRequest.logo,
          userRequest.title,
          userRequest.job_description,
          userRequest.job_function,
          userRequest.industry,
          userRequest.job_location,
          userRequest.cover_image,
          userRequest.employment_type,
          userRequest.level,
          userRequest.views,
          BSONObjectID.generate().stringify,""
        )
      }
      response <- insert[Job](updateCollection, jobData)
    }
      yield (response)
  }
*/
  def insertNewJobFeed(userRequest: JobRequest): Future[FeedJob] = {
    for {
      feedData <- Future {
        FeedJob(BSONObjectID.generate().stringify, userRequest.memberID,
          "Job",
          Job(userRequest.memberID,active,
            userRequest.coyID, userRequest.jobID,
            userRequest.company_name, userRequest.company_url, userRequest.about_us, userRequest.company_size, 
            userRequest.logo, userRequest.title, userRequest.job_description, userRequest.job_function, 
            userRequest.industry, userRequest.job_location, userRequest.cover_image,
            userRequest.employment_type, userRequest.level, userRequest.views,
            DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),""
          ),
          None, None, None, None
        )
      }
      response <- insert[FeedJob](feedCollection, feedData)
    }
      yield (response)
  }
  
  
  //update user Details
  def updateUpdateFeed(userRequest: Update, feedType: String): Future[String] = {

    val selector = BSONDocument("postDetails.postID" -> userRequest.postID, "activityType" -> "Update")
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

  //Get all like for Post
  def getAllLikesStore(postID: String): Future[List[Post]] = {
    searchAll[Post](postCollection,
      BSONDocument("postID" -> postID, "status" -> active))
  }

  //LikePost
  def LikePost(userRequest: LikePostRequest): Future[String] = {
    for {

      response <- update(postCollection, BSONDocument("postID" -> userRequest.postID, "status" -> active), BSONDocument("$addToSet" -> BSONDocument("likes" -> LikeDetails(userRequest.actorID, userRequest.like,
        DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")))))
    }
      yield (response)
  }

  //UnLikePost
  def UnLikePost(postID: String, memberID: String): Future[String] = {
    for {

      response <- update(postCollection, BSONDocument("postID" -> postID, "status" -> active), BSONDocument("$pull" -> BSONDocument("likes" -> BSONDocument("likeID" -> memberID))))
    }
      yield (response)
  }


  def updateNewSharePost(userRequest: PostShare): Future[String] = {
    for {

      response <- update(postCollection, BSONDocument("postID" -> userRequest.postID, "status" -> active
      ), BSONDocument("$addToSet" -> BSONDocument("shares" -> BSONDocument("postID" -> userRequest.postID, "memberID" -> userRequest.memberID, "recipients" -> userRequest.recipients))))
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
          userRequest.postID,
          userRequest.comment_text,
          DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),
          "",
          userRequest.replies,
          None
        )
      }
      response <- insert[Comment](commentCollection, postData)
    }
      yield (response)
  }

  //Get Comment
  def getComment(postID: String): Future[List[Comment]] = {
    searchAll[Comment](commentCollection,
      document("postID" -> postID, "status" -> active))
  }


  def getCommentCount(postID: String): Future[List[Comment]] = {
    searchAll[Comment](commentCollection,
      document("postID" -> postID, "status" -> active))
  }

  def getFeedForComment(userRequest: LikeCommentRequest): Future[Option[Feed]] = {
    search[Feed](feedCollection,
      document("commentID" -> userRequest.commentID, "activityType" -> "Comment"))
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
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,
          feedType,
          Post(userRequest.memberID,active,
            userRequest.postID,
            DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
            userRequest.title, userRequest.description, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.author_position,
            userRequest.author_current_employer, userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.post_url, userRequest.cover_image, userRequest.readers, None,
            None
          ),
          None, None, None, None
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)


  }

  def insertNewCommentFeed(userRequest: CommentRequest, feedType: String, commentResp: Comment): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,
          feedType,
          Post(userRequest.memberID,active,
            userRequest.postID,
            DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
            userRequest.title, userRequest.description, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.author_position,
            userRequest.author_current_employer, userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.post_url, userRequest.cover_image, userRequest.readers, None,
            None
          ),
          Some(userRequest.actorID),
          userRequest.actorName,
          userRequest.actorAvatar, Some(commentResp.commentID)
        )
      }
      response <- insert[Feed](feedCollection, feedData)
      
    }
      yield (response)
      

  }


  def insertNewLikeFeed(userRequest: LikePostRequest, feedType: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,
          feedType,
          Post(userRequest.memberID,active,
            userRequest.postID,
            DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"),"",
            userRequest.title, userRequest.description, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.author_position,
            userRequest.author_current_employer, userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.post_url, userRequest.cover_image, userRequest.readers, None,
            None
          ),
          Some(userRequest.actorID),
          userRequest.actorName,
          userRequest.actorAvatar, None
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)


  }

  def insertLikeFeedForComment(userRequest: Feed, feedType: String, memberID: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,
          feedType,
          userRequest.postDetails,
          Some(memberID),
          None,
          None, userRequest.commentID
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)


  }

  //like Comment
  def LikeComment(userRequest: LikeCommentRequest): Future[String] = {
    for {

      response <- update(commentCollection, BSONDocument("commentID" -> userRequest.commentID, "status" -> active), BSONDocument("$addToSet" -> BSONDocument("likes" -> userRequest.memberID)))
    }
      yield (response)
  }

  //Unlike Comment
  def UnLikeComment(commentID: String, memberID: String): Future[String] = {
    for {

      response <- update(commentCollection, BSONDocument("commentID" -> commentID, "status" -> active), BSONDocument("$pull" -> BSONDocument("likes" -> memberID)))
    }
      yield (response)
  }


  private def getPaginationSort(page: PageRequest) = {
    val sort: BSONDocument = BSONDocument(page.sort.map {
      case (k, Asc) => (k, BSONInteger(1))
      case (k, Desc) => (k, BSONInteger(-1))
    })
    sort
  }

  def insertNewReply(userRequest: ReplyRequest): Future[Reply] = {

    for {
      replyData <- Future {
        Reply(BSONObjectID.generate().stringify,
          userRequest.commentID,
          userRequest.actorID,
          userRequest.actorName,
          userRequest.actorAvatar, userRequest.reply_text,
          DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")
        )
      }
      response <- insert[Reply](replytCollection, replyData)
    }
      yield (response)
  }


  def getreply(commentID: String): Future[List[Reply]] = {
    searchAll[Reply](replytCollection,
      document("commentID" -> commentID))
  }

  def getUpdateDetailsByID(memberID: String, coyID: String): Future[List[Update]] = {
    searchAll[Update](updateCollection,
      document("memberID" -> memberID, "coyID" -> coyID, "status" -> active))
  }

  def getOneUpdateDetails(memberID: String, postID: String): Future[Option[Update]] = {
    search[Update](updateCollection,
      document("memberID" -> memberID, "postID" -> postID, "status" -> active))
  }


}


