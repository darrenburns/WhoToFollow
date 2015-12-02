package actors

import actors.FeatureExtraction.TweetFeatures
import actors.QueryHandler.FetchLatestQueryExperts
import actors.RedisWriter.{NewQuery, TweetQualityReportBatch}
import actors.UserHashtagCounter.{UserHashtagCount, UserHashtagReport}
import akka.actor.Actor
import com.google.inject.Singleton
import init.RedisConnectionPool
import play.api.Logger


/*
 Reports can be sent here for storage in Redis.
 */
object RedisWriter {
  case class NewQuery(query: String)
  case class HashtagCountUpdate(results: Seq[UserHashtagCount])
  case class TweetQualityReportBatch(reports: Seq[TweetFeatures])
}

@Singleton
class RedisWriter extends Actor with Serializable {

  private val clients = RedisConnectionPool.pool

  override def receive = {
    case UserHashtagReport(results) =>
      applyHashtagCounts(results)
    case TweetQualityReportBatch(reports) =>
      Logger.debug("RedisWriter - received TweetQualityReportBatch")
      updateExtractedFeatures(reports)
    case NewQuery(q) =>
      addQuery(q)


  }

  /**
    * Adds the query to the list of recent queries
    */
  def addQuery(query: String) = {
    clients.withClient{client =>
      client.lpush("recentQueries", query)
    }
  }

  /** Increments the hashtag counts stored in Redis to the latest correct value.
   *
   * @param hashtagCounts The sequence of UserHashtagCount objects to update Redis with.
   */
  def applyHashtagCounts(hashtagCounts: Seq[UserHashtagCount]): Unit = {
    clients.withClient{client =>
      hashtagCounts.foreach(userTagCount => {
        client.zincrby(s"hashtags:${userTagCount.hashtag}", userTagCount.count, userTagCount.username)
      })
    }
  }

  /** Updates user models based on the features extracted
   *
   * @param reports The sequence of TweetQualityReports to update Redis with.
   */
  def updateExtractedFeatures(reports: Seq[TweetFeatures]): Unit = {
    /*
    A report looks like this:

      case class TweetQualityReport(username: String, followerCount: Int, punctuationCounts: Map[Char, Int],
                                wordCount: Int, capWordCount: Int, hashtagCount: Int, retweetCount: Long,
                                mentionCount: Int, likeCount: Int, dictionaryHits: Int, linkCount: Int)

     */
    reports.foreach(report => {
      val user = report.username
      clients.withClient{client =>
        // Increment the value at <username>:stats:tweetCount by 1
        client.hincrby(s"user:$user:stats", "tweetCount", 1)
        // Set the value at <username>:stats:followerCount to report.followerCount
        client.hset(s"user:$user:stats", "followerCount", report.followerCount)
        // Increment the values at <username>:stats:<char> by report.punctuationCounts
        report.punctuationCounts.foreach(count => {
          client.hincrby(s"user:$user:stats", s"${count._1}", count._2)
        })
        // Increment the value at <username>:stats:wordCount by report.wordCount
        client.hincrby(s"user:$user:stats", "wordCount", report.wordCount)
        // Increment the value at <username>:stats:capWordCount by report.capWordCount
        client.hincrby(s"user:$user:stats", "capWordCount", report.capWordCount)
        // Increment the value at <username>:stats:hashtagCount by report.hashtagCount
        client.hincrby(s"user:$user:stats", "hashtagCount", report.hashtagCount)
        // Increment the value at <username>:stats:retweetCount by report.retweetCount
        client.hincrby(s"user:$user:stats", "retweetCount", report.retweetCount)
        // Increment the value at <username>:stats:likeCount by report.likeCount
        client.hincrby(s"user:$user:stats", "likeCount", report.likeCount)
        // Increment the value at <username>:stats:dictionaryHits
        client.hincrby(s"user:$user:stats", "dictionaryHits", report.dictionaryHits)
        // Increment the link count
        client.hincrby(s"user:$user:stats", "linkCount", report.linkCount)
      }
    })
  }

}

