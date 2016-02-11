package persist.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import akka.routing.RoundRobinPool
import channels.actors.{MetricsReporting, UserChannelSupervisor}
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import persist.actors.RedisQueryWorker.RedisQuery
import persist.actors.RedisWriterWorker.RedisWriteRequest
import play.api.Logger


object RedisActor {
  final val name = "RedisActor"
}

/**
  * Routes Redis related tasks to load-balanced pools of workers.
  *
  * See 'Akka Routers' for more information.
  */
@Singleton
class RedisActor @Inject() (
  @Named(UserChannelSupervisor.name) userChannelSupervisor: ActorRef,
  @Named(MetricsReporting.name) metricsReporting: ActorRef
) extends Actor {

  // Define routers for load-balancing balanced requests
  val queryWorkerPool = context.actorOf(RoundRobinPool(4).props(RedisQueryWorker.props(userChannelSupervisor, metricsReporting)), "queryWorkerPool")
  val writingWorkerPool = context.actorOf(RoundRobinPool(4).props(Props[RedisWriterWorker]), "writingWorkerPool")

  def receive = LoggingReceive {
    /*
     Queries can come from worker actors. Therefore the `sender` reference can change over time,
     and should never be relied on. See "Cameo pattern" for information on how to retain correct
     `sender` reference.
     */
    case query: RedisQuery =>
      Logger.debug("RedisActor received new RedisQuery")
      queryWorkerPool forward query
    // All requests to write data to Redis are 'fire-and-forget'
    case write: RedisWriteRequest =>
      writingWorkerPool ! write

  }

}
