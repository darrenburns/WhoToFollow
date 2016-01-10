package persist.actors

import akka.actor.Actor
import com.mongodb.casbah.Imports._
import hooks.MongoInit
import persist.actors.UserMetadataWriter.{UserMetadataQuery, TwitterUser}
import play.api.Logger
import play.api.libs.json.{Json, Writes}

object UserMetadataReader {

  lazy val db = MongoInit.db
  lazy val collection = db("usermeta")

  case class UserMetadata(screenName: String, name: String, avatarUrl: String, coverUrl: String, profileColour: String)

  def fromDbObject(obj: collection.T): UserMetadata = {
    UserMetadata(
      obj.getAsOrElse[String]("screenName", "unknown"),
      obj.getAsOrElse[String]("name", "unknown"),
      obj.getAsOrElse[String]("avatarUrl", "unknown"),
      obj.getAsOrElse[String]("coverUrl", "unknown"),
      obj.getAsOrElse[String]("profileColour", "#EFEFEF")
    )
  }

  implicit val userMetadataWrites = new Writes[UserMetadata] {
    def writes(metadata: UserMetadata) =
      Json.obj(
        "screenName" -> metadata.screenName,
        "name" -> metadata.name,
        "avatarUrl" -> metadata.avatarUrl,
        "coverPhotoUrl" -> metadata.coverUrl,
        "profileColour" -> metadata.profileColour
      )
  }
}

class UserMetadataReader extends Actor {

  import UserMetadataReader._

  override def receive = {

    /* Fetch existing metadata for a user */
    case UserMetadataQuery(screenName: String) =>
      val metaObj = collection.findOne(
        MongoDBObject(
          "screenName" -> screenName
        )
      ) match {
        case Some(meta) =>
          val userMetadata = fromDbObject(meta)
          sender ! userMetadata
        case None => Logger.error(s"Unable to find metadata for $screenName.")
      }

  }

}
