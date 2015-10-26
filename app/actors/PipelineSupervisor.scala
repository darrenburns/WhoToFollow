package actors

import actors.HashtagCounter.{HashtagCount, ActiveTwitterStream}
import akka.actor.{ActorRef, Actor}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}

/*
 The mediator of all analytical tasks. Since Spark streaming
 often has to block, want to run analysis in individual actors.
 Any process in the pipeline can send results here for storage
 and distribution.
 */


object PipelineSupervisor {
  case class HashtagCountUpdate(results: List[HashtagCount])
}

@Singleton
class PipelineSupervisor @Inject()
  (@Named("hashtagCounter") hashtagCounter: ActorRef,
   @Named("webSocketSupervisor") webSocketSupervisor: ActorRef,
   @Named("tweetStreamActor") tweetStreamActor: ActorRef) extends Actor {

  import PipelineSupervisor._

  // Todo: Store result of updates somewhere.
  override def receive = {
    // Send the ActiveTwitterStream to anywhere that needs it.
    case stream @ ActiveTwitterStream(handle) =>
      hashtagCounter forward stream

    case update @ HashtagCountUpdate(results) =>
      webSocketSupervisor forward update
  }

}

