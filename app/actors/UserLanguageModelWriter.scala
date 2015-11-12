package actors

import actors.UserLanguageModelling.UserModelUpdate
import akka.actor.Actor
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import init.Mongo


class UserLanguageModelWriter extends Actor {

  val mongoClient = Mongo.db

  override def receive = {

    case UserModelUpdate(wordCounts) =>
      val collection = mongoClient("users")
      val parallel = collection.initializeUnorderedBulkOperation
      wordCounts.foreach(count => {
        val userModel = MongoDBObject("user" -> count.username)
        parallel.find(userModel).update($set("word" -> count.word))
        parallel.find(userModel).update($inc("count" -> count.count))
      })
      parallel.execute()

  }

}
