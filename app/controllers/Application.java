//package controllers;
//
//import actors.WebSocketSupervisor;
//import akka.actor.ActorRef;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.google.inject.Inject;
//import com.google.inject.name.Named;
//import com.fasterxml.jackson.databind.JsonNode;
//import play.libs.F;
//import play.mvc.Controller;
//import play.mvc.Result;
//import play.mvc.WebSocket;
//
//public class Application extends Controller {
//
//    private ActorRef websocketSupervisor;
//
//    @Inject
//    public Application(@Named("webSocketSupervisor") ActorRef websocketSupervisor) {
//        System.out.println("Injecting webSocketSupervisor");
//        this.websocketSupervisor = websocketSupervisor;
//    }
//
//    public Result index() {
//        return ok(views.html.index.render());
//    }
//
//
//    /*    TODO: Idea for handling sockets
//    We can create a generic method listenToQueryStream(queryText: String),
//    The query text will be sent to QuerySupervisor, who will look up the query in a HashMap
//    (queryString -> QueryActor). If an Actor doesn't already exist for handling this query,
//    then one is created. The creation of a QueryActor involves initialising a new WebSocket
//    and continuously polling SOMEWHERE for updates (every 5 seconds, say). Regardless of the
//    situation here, the newly created or old WebSocket handle must be sent back here so it can
//    be returned.
//     */
//    public WebSocket<JsonNode> hashtagCountStream() {
//        return new WebSocket<JsonNode>() {
//            public void onReady(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
//                websocketSupervisor.tell(new WebSocketSupervisor.OutputChannel("test"), null);
//            }
//        };
//    }
//
//
//
//}