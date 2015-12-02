package actors

import java.util

import actors.TweetStreamActor.Ready
import actors.UserHashtagCounter.ActiveTwitterStream
import akka.actor.Actor
import com.google.inject.Singleton
import org.apache.commons.io.IOUtils
import org.terrier.indexing.TaggedDocument
import org.terrier.indexing.tokenisation.Tokeniser
import org.terrier.realtime.memory.MemoryIndex
import play.api.Logger

/*
  Handles Terrier indexing of streaming tweets in real-time
 */
@Singleton
class UserIndexing extends Actor {

  val index = new MemoryIndex()
  val tokeniser = Tokeniser.getTokeniser

  override def receive = {
    case ActiveTwitterStream(stream) =>
      Logger.info("UserIndexing starting")
      stream.foreachRDD(statusBatch => {
        statusBatch.foreach(status => {
          val userId = status.getUser.getId.toInt // The key for a user document is that user's Twitter ID

          val trecStatus = s"<DOC><DOCNO>$userId</DOCNO>${status.getText}</DOC>"

          val doc = new TaggedDocument(IOUtils.toInputStream(trecStatus, "UTF-8"),
            new util.HashMap[String, String](), tokeniser)

          if (index.getDocumentIndex.getDocumentEntry(userId) != null) {
            index.addToDocument(userId, doc)
          } else {
            index.indexDocument(doc)
          }

        })
      })
      Logger.info("UserIndexing sending Ready")
      sender ! Ready()  // Recipient: TweetStreamActor
    }

}
