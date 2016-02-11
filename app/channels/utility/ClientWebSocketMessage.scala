package channels.utility

sealed trait ClientWebSocketMessage { def messageType: String }

case object KeepAlive extends ClientWebSocketMessage {
  val messageType = "KEEP-ALIVE"
}
