package utils

import org.slf4j.LoggerFactory

trait TwitterAuth {

  val logger = LoggerFactory.getLogger(getClass)

  /*
   Authenticates application with Twitter.
   */
  def checkTwitterKeys(): Unit = {
    sys.env.get("WTF_TWITTER_CONSUMER_KEY") match {
      case Some(key) => System.setProperty("twitter4j.oauth.consumerKey", key)
      case None => logger.error("WTF_TWITTER_CONSUMER_KEY environment variable not set.")
    }

    sys.env.get("WTF_TWITTER_CONSUMER_SECRET") match {
      case Some(secret) => System.setProperty("twitter4j.oauth.consumerSecret", secret)
      case None => logger.error("WTF_TWITTER_CONSUMER_SECRET environment variable not set.")
    }

    sys.env.get("WTF_TWITTER_ACCESS_TOKEN") match {
      case Some(accessToken) => System.setProperty("twitter4j.oauth.accessToken", accessToken)
      case None => logger.error("WTF_TWITTER_ACCESS_TOKEN environment variable not set.")
    }

    sys.env.get("WTF_TWITTER_ACCESS_TOKEN_SECRET") match {
      case Some(secret) => System.setProperty("twitter4j.oauth.accessTokenSecret", secret)
      case None => logger.error("WTF_TWITTER_ACCESS_TOKEN_SECRET environment variable not set.")
    }
  }

}
