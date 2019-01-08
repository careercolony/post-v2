package com.mj.users.mongo

import com.mj.users.directive.Order.{Asc, Desc}
import com.mj.users.directive.PageRequest
import com.mj.users.model._
import com.mj.users.mongo.MongoConnector._
import com.mj.users.mongo.Neo4jConnector.getNeo4j
import com.mj.users.mongo.PostDao.retrieveFriendsPosts
import org.neo4j.driver.v1.{AuthTokens, GraphDatabase, Session}
import reactivemongo.api.QueryOpts
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._

import scala.concurrent.Future

object PostDao {


  val postCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("post"))

  val feedCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("feed"))
  val commentCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("comment"))

  implicit def PostRequestWriter = Macros.handler[PostRequest]

  implicit def likeDetailsWriter = Macros.handler[LikeDetails]

  implicit def postWriter = Macros.handler[Post]

  implicit def feedWriter = Macros.handler[Feed]

  implicit def commentWriter = Macros.handler[Comment]

  //Get all Feed
  def getFeedStore(): Future[List[Feed]] = {

    val result = searchAll[Feed](feedCollection,
      BSONDocument())
    println("result:"+result)
    result
  }

  //Get All Post
  def getMemberIDStore(memberID: String, page: PageRequest): Future[List[Post]] = {
    val sort: BSONDocument = getPaginationSort(page)
    val queryOps = QueryOpts(skipN = page.offset, batchSizeN = page.limit, flagsN = 0)
    searchWithPagination[Post](postCollection,
      BSONDocument("memberID" -> memberID), queryOps, sort, page.limit)
  }

  //Get all like for Post
  def getAllLikesStore(postID: String): Future[List[Post]] = {
    searchAll[Post](postCollection,
      BSONDocument("postID" -> postID))
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
   Future{List()}
    getFriends(memberID).flatMap(resp =>
      retrieveFriendsPosts(resp.map(_.memberID), None).mapTo[List[Feed]]
    )/*_.filterNot(_.postDetails.readers.exists(_.contains(memberID.toString))*/

  }

  def retrieveFriendsPosts(listOfMemberId: List[String], pageOpt: Option[PageRequest]): Future[List[Feed]] = {
    val page : PageRequest = pageOpt.getOrElse(PageRequest.default.copy(sort = Map("postID" -> Desc)))
    val sort : BSONDocument = getPaginationSort(page)
    val queryOps = QueryOpts(skipN = page.offset, batchSizeN = page.limit, flagsN = 0)
    /*searchWithPagination[Feed](feedCollection,
      BSONDocument("memberID" -> BSONDocument("$in" -> listOfMemberId)), queryOps, sort, page.limit)*/
    println("listOfMemberId:"+listOfMemberId(0).toString.substring(1, listOfMemberId(0).toString.length()-1))
 /*   val friendList : String =if(listOfMemberId.size == 1)
      "[" + listOfMemberId(0) + "]"
     else
      if(listOfMemberId.nonEmpty) "[" + listOfMemberId.map(_.toString).reduce(_+","+_) + "]" else "[]"
    println("listOfMemberId:"+friendList)*/
    val selector = BSONDocument("memberID" -> /*BSONDocument("$in" -> listOfMemberId)*/listOfMemberId(0).toString.substring(1, listOfMemberId(0).toString.length()-1))
    searchAll[Feed](feedCollection,selector)
  }

  //insert user Details
  def insertNewPost(userRequest: PostRequest): Future[Post] = {
    for {
      postData <- Future {
        Post(userRequest.memberID,
          BSONObjectID.generate().stringify,
          "",
          userRequest.title, userRequest.description, userRequest.message, userRequest.post_type,
          userRequest.author, userRequest.author_avatar, userRequest.author_position,
          userRequest.author_current_employer, userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
          userRequest.post_url, userRequest.html, userRequest.readers, None,
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

      response <- updateDetails[Post](postCollection, BSONDocument("postID" -> userRequest.postID), userRequest)
    }
      yield (response)
  }

  //LikePost
  def LikePost(userRequest: LikePostRequest): Future[String] = {
    for {

      response <- update(postCollection, BSONDocument("postID" -> userRequest.postID), BSONDocument("$addToSet" -> BSONDocument("likes" -> LikeDetails(userRequest.actorID, userRequest.like, ""))))
    }
      yield (response)
  }

  //UnLikePost
  def UnLikePost(postID: String, memberID: String): Future[String] = {
    for {

      response <- update(postCollection, BSONDocument("postID" -> postID), BSONDocument("$pull" -> BSONDocument("likes" -> BSONDocument("likeID" -> memberID) )))
    }
      yield (response)
  }


  def updateNewSharePost(userRequest: PostShare): Future[String] = {
    for {

      response <- update(postCollection, BSONDocument("postID" -> userRequest.postID), BSONDocument("$addToSet" -> BSONDocument("shares" -> userRequest.memberID)))
    }
      yield (response)
  }


  //insert comment
  def insertNewComment(userRequest: CommentRequest): Future[Comment] = {
    for {
      postData <- Future {
        Comment(BSONObjectID.generate().stringify,
          userRequest.memberID,
          userRequest.actorAvatar,
          userRequest.actorName,
          userRequest.postID,
          userRequest.comment_text,
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
      document("postID" -> postID))
  }

  def getCommentCount(postID: String): Future[List[Comment]] = {
    searchAll[Comment](commentCollection,
      document("postID" -> postID))
  }

  //like Comment
  def LikeComment(userRequest: LikeCommentRequest): Future[String] = {
    for {

      response <- update(commentCollection, BSONDocument("commentID" -> userRequest.commentID), BSONDocument("$addToSet" -> BSONDocument("likes" -> userRequest.memberID)))
    }
      yield (response)
  }

  //Unlike Comment
  def UnLikeComment(commentID: String, memberID: String): Future[String] = {
    for {

      response <- update(commentCollection, BSONDocument("commentID" -> commentID), BSONDocument("$pull" -> BSONDocument("likes" -> memberID)))
    }
      yield (response)
  }


  //update user Details
  def updateNewFeed(userRequest: Post, feedType: String): Future[String] = {

    val selector = BSONDocument("postDetails.postID" -> userRequest.postID,"activityType" -> "Post")
    val result = for {

      response <- update(feedCollection, selector, BSONDocument(
        "$set" -> BSONDocument("postDetails" -> userRequest)
      ))
    }
      yield (response)

    result.recover {
      case e: Throwable => {
        println("msg:"+e.getMessage)
        throw new Exception(e.getMessage)
      }


    }
  }


  def insertNewPostFeed(userRequest: Post, feedType: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,
          feedType,
          Post(userRequest.memberID,
            userRequest.postID,
            "",
            userRequest.title, userRequest.description, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.author_position,
            userRequest.author_current_employer, userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.post_url, userRequest.html, userRequest.readers, None,
            None
          ),
          None, None, None
        )
      }
      response <- insert[Feed](feedCollection, feedData)
    }
      yield (response)


  }

  def insertNewCommentFeed(userRequest: CommentRequest, feedType: String): Future[Feed] = {
    for {
      feedData <- Future {
        Feed(BSONObjectID.generate().stringify, userRequest.memberID,
          feedType,
          Post(userRequest.memberID,
            userRequest.postID,
            "",
            userRequest.title, userRequest.description, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.author_position,
            userRequest.author_current_employer, userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.post_url, userRequest.html, userRequest.readers, None,
            None
          ),
          Some(userRequest.actorID),
          userRequest.actorName,
          userRequest.actorAvatar
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
          Post(userRequest.memberID,
            userRequest.postID,
            "",
            userRequest.title, userRequest.description, userRequest.message, userRequest.post_type,
            userRequest.author, userRequest.author_avatar, userRequest.author_position,
            userRequest.author_current_employer, userRequest.thumbnail_url, userRequest.provider_name, userRequest.provider_url,
            userRequest.post_url, userRequest.html, userRequest.readers, None,
            None
          ),
          Some(userRequest.actorID),
          userRequest.actorName,
          userRequest.actorAvatar
        )
      }
      response <- insert[Feed](feedCollection, feedData)
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


}
