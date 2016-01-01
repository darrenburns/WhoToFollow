package learn.utility

import learn.actors.FeatureExtraction.TweetFeatures
import learn.actors.UserHashtagCounter.{UserHashtagCount, UserHashtagReport}
import org.apache.commons.net.ntp.TimeStamp
import twitter4j.Status
import utils.QualityAnalyser

/* TODO
 Look at example in slides for querying an incremental index.
 Use Terrier scores as additional feature.
 Temporal features
 */

object ExtractionUtils {

  def getStatusFeatures(status: Status): TweetFeatures = {
    val qa = new QualityAnalyser(status.getText)
    val htCount = status.getHashtagEntities.length
    val mentionCount = status.getUserMentionEntities.length
    val linkCount = status.getURLEntities.length
    TweetFeatures(
      status = status,
      id = status.getId,
      username = status.getUser.getScreenName,
      followerCount = status.getUser.getFollowersCount,
      punctuationCounts = qa.findPunctuationCounts(),
      wordCount = qa.countWords() - htCount - mentionCount - linkCount,
      capWordCount = qa.countCapitalisedWords(),
      hashtagCount = htCount,
      retweetCount = status.getRetweetCount,
      mentionCount = mentionCount,
      likeCount = status.getFavoriteCount,
      dictionaryHits = qa.countDictionaryHits(),
      linkCount = linkCount
    )
  }

  def getHashtagCounts(status: Status): UserHashtagReport = {
    UserHashtagReport(status.getHashtagEntities.map(hashtag => {
      // TODO: Ensure this gets the hashtag in the expected form. It probably won't.
      // Might be worth changing how they're stored in Redis when this is fixed.
        UserHashtagCount(status.getUser.getScreenName, hashtag.getText, 1)
    }))
  }

  def fetchAndAnalyseTimeline(screenName: String): Unit = {

  }

}