package utils

import org.slf4j.LoggerFactory
import play.api.Logger

trait TwitterAuth {

  /*
   Authenticates application with Twitter.
   */
  def checkTwitterKeys(): Unit = {
    sys.env.get("WTF_TWITTER_CONSUMER_KEY") match {
      case Some(key) =>
        Logger.debug("Setting Twitter consumer key")
        System.setProperty("twitter4j.oauth.consumerKey", key)
      case None => Logger.error("WTF_TWITTER_CONSUMER_KEY environment variable not set.")
    }

    sys.env.get("WTF_TWITTER_CONSUMER_SECRET") match {
      case Some(secret) =>
        Logger.debug("Setting Twitter consumer secret")
        System.setProperty("twitter4j.oauth.consumerSecret", secret)
      case None => Logger.error("WTF_TWITTER_CONSUMER_SECRET environment variable not set.")
    }

    sys.env.get("WTF_TWITTER_ACCESS_TOKEN") match {
      case Some(accessToken) =>
        Logger.debug("Setting Twitter access token")
        System.setProperty("twitter4j.oauth.accessToken", accessToken)
      case None => Logger.error("WTF_TWITTER_ACCESS_TOKEN environment variable not set.")
    }

    sys.env.get("WTF_TWITTER_ACCESS_TOKEN_SECRET") match {
      case Some(secret) =>
        Logger.debug("Setting Twitter access secret")
        System.setProperty("twitter4j.oauth.accessTokenSecret", secret)
      case None => Logger.error("WTF_TWITTER_ACCESS_TOKEN_SECRET environment variable not set.")
    }
  }

}
