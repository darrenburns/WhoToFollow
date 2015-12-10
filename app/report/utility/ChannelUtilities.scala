package report.utility

/**
  * Object contains utility methods for dealing with channels. Mostly deals with management of channel names,
  * although not all naming conventions have been moved here yet.
  */
object ChannelUtilities {

  /**
    * @param channelName The name of the channel to test
    * @return True if the channel is a user analysis stream (rather than a query). False otherwise.
    */
  def isUserAnalysisChannel(channelName: String): Boolean = channelName.startsWith("user:")

  /**
    * @param channelName The name of the channel to test
    * @return True if the channel is a query channel and not a default stream or an individual user info stream. False
    *         otherwise.
    */
  def isQueryChannel(channelName: String): Boolean =
    !(channelName.startsWith("user:") || channelName.startsWith("default:"))


  def getScreenNameFromChannelName(channelName: String): Option[String] = {
    isUserAnalysisChannel(channelName) match {
      case true => Some(channelName.substring(5))
      case false => None
    }
  }

  def getChannelNameFromScreenName(screenName: String): String = s"user:$screenName"
}
