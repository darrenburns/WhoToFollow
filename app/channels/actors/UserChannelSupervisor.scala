package channels.actors

import akka.actor.Actor
import channels.actors.ChannelMessages.{CloseChannel, CreateChannel}
import com.google.inject.{Singleton, Inject}
import di.NamedActor
import learn.actors.FeatureExtraction.UserFeatures
import play.api.Logger
import play.api.libs.concurrent.InjectedActorSupport

import scala.collection.immutable.HashMap


object UserChannelSupervisor extends NamedActor {
  final val name = "UserChannelSupervisor"
}

@Singleton
class UserChannelSupervisor @Inject() (
  workerFactory: UserChannelWorker.Factory
) extends Actor with InjectedActorSupport with GenericChannel.Supervisor {

  override var channels: HashMap[String, ChannelMeta] = HashMap.empty

  override def receive = {
    case CreateChannel(screenName: String) =>
      val channelMeta = getOrCreateChannel(screenName, workerFactory)
      sender ! (channelMeta match {
        case meta: ChannelMeta => Right((meta.in, meta.out))
        case _ => Left(s"Error creating/fetching channel for user '$screenName'.")
      })
    case CloseChannel(screenName: String) => closeChannel(screenName)

    /*
    Features for users are first sent here and then passed on to the correct actor
    in order to prevent bugs relating the changing sender refs.
     */
    case userFeatures: UserFeatures =>
      Logger.debug("UserChannelSupervisor sending UserFeatures to relevant worker.")
      channels.get(userFeatures.screenName) match {
        case Some(meta) => meta.actor ! userFeatures
      }
  }

}
