package persist.actors

import akka.actor.Actor
import com.mongodb.casbah.Imports._
import hooks.MongoInit
import play.api.Logger



object UserMetadataWriter {
  lazy val db = MongoInit.db
  lazy val collection = db("usermeta")

  case class UserMetadataQuery(screenName: String)

  case class TwitterUser(user: twitter4j.User)
}

class UserMetadataWriter extends Actor {

  import UserMetadataWriter._

  override def receive = {

    /* Store new metadata for a user */
    case TwitterUser(user) =>
      val screenName = user.getScreenName
      collection.update(
        MongoDBObject(
          "screenName" -> screenName
        ),
        MongoDBObject(
          "screenName" -> screenName,
          "name" -> user.getName,
          "avatarUrl" -> user.getOriginalProfileImageURL,
          "coverUrl" -> user.getProfileBannerURL,
          "profileColour" -> user.getProfileBackgroundColor
        ),
        upsert=true
      )

  }

}
