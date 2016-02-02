package persist.actors

import akka.actor.{Props, Actor}
import akka.routing.BalancingPool
import persist.actors.RedisQueryWorker.RedisQuery
import persist.actors.RedisWriterWorker.RedisWriteRequest


/**
  * Routes Redis related tasks to load-balanced pools of workers.
  */
class RedisActor extends Actor {

  val queryWorkerPool = context.actorOf(BalancingPool(3).props(Props[RedisQueryWorker]), "queryWorkerPool")
  val writingWorkerPool = context.actorOf(BalancingPool(3).props(Props[RedisWriterWorker]), "writingWorkerPool")

  def receive = {
    // Decide which worker pool will handle the incoming request.
    case query: RedisQuery => queryWorkerPool forward query
    case write: RedisWriteRequest => writingWorkerPool forward write

  }

}
