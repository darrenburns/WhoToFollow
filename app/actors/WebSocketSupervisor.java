///*
// Manages the output of JSON through a WebSocket handle.
// */
//
//package actors;
//
//import akka.actor.UntypedActor;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.google.inject.Singleton;
//import models.WordCount;
//import play.libs.Json;
//import play.mvc.WebSocket;
//import scala.collection.JavaConversions;
//import scala.collection.immutable.HashMap;
//
//import java.util.List;
//
//// TODO: Rewrite this in Scala
//
//@Singleton
//public class WebSocketSupervisor extends UntypedActor {
//
//
//
//    public WebSocketSupervisor(WebSocket.Out<JsonNode> out) {
//        System.out.println("Constructed new UserActor");
//        this.out = out;
//    }
//
//    @Override
//    public void onReceive(Object message) throws Exception {
//        System.out.println("UserActor received a message: " + message);
//        if (message instanceof PipelineSupervisor.WordCountUpdate) {
//            PipelineSupervisor.WordCountUpdate update =
//                    (PipelineSupervisor.WordCountUpdate) message;
//            ObjectNode outputJson = Json.newObject();
//            ArrayNode dataJsonArray = outputJson.putArray("data");
//            List<WordCount> results = JavaConversions.asJavaList(update.results());
//            ObjectNode wordCountObject = Json.newObject();
//            for (WordCount result : results) {
//                wordCountObject.set("word", Json.toJson(result.word));
//                wordCountObject.set("count", Json.toJson(result.count));
//                dataJsonArray.add(wordCountObject);
//            }
//            out.write(outputJson);
//        }
//    }
//}
