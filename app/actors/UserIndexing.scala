package actors

import actors.UserHashtagCounter.ActiveTwitterStream
import akka.actor.Actor
import com.google.gson.JsonParser
import com.google.inject.Singleton
import org.terrier.indexing.TwitterJSONDocument
import org.terrier.realtime.memory.MemoryIndex
import twitter4j.json.DataObjectFactory

@Singleton
class UserIndexing extends Actor {

//  val index = new MemoryIndex()

  override def receive = {
    case ActiveTwitterStream(stream) => {
//      stream.foreachRDD(statusBatch => {
//        statusBatch.foreach(status => {
//          val statusJson = DataObjectFactory.getRawJSON(status)
//          val json = new JsonParser().parse(statusJson).getAsJsonObject
//
//          val userId = status.getUser.getId.toInt // The key for a user document is that user's Twitter ID
//
//          // If the user document already exists, append the tweet to their document
//          if (index.getDocumentIndex.getDocumentEntry(userId) != null) {
//            index.addToDocument(userId, new TwitterJSONDocument(json))
//          } else {
//            // Otherwise insert a new document for that user containing just the tweet
//            index.indexDocument(new TwitterJSONDocument(json))
//          }
//        })
//      })
    }
  }

}
