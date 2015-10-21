package actors

import actors.WordCountActor.{WordCount, ActiveTwitterStream}
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
  case class WordCountUpdate(results: List[WordCount])
}

@Singleton
class PipelineSupervisor @Inject()
  (@Named("wordCountActor") wordCountActor: ActorRef,
   @Named("webSocketSupervisor") webSocketSupervisor: ActorRef,
   @Named("tweetStreamActor") tweetStreamActor: ActorRef) extends Actor {

  import PipelineSupervisor._

  // Todo: Store result of updates somewhere.
  override def receive = {
    // Send the ActiveTwitterStream to anywhere that needs it.
    case stream @ ActiveTwitterStream(handle) =>
      wordCountActor forward stream

    case update @ WordCountUpdate(results) =>
      webSocketSupervisor forward update
  }

}

