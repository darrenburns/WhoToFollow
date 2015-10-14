package actors;

import akka.actor.UntypedActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.WordCount;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.collection.JavaConversions;

import java.util.List;

public class UserActor extends UntypedActor {

    private final WebSocket.Out<JsonNode> out;

    public UserActor(WebSocket.Out<JsonNode> out) {
        System.out.println("Constructed new UserActor");
        this.out = out;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        System.out.println("UserActor received a message: " + message);
        if (message instanceof ResultUpdate) {
            ResultUpdate update = (ResultUpdate) message;
            ObjectNode outputJson = Json.newObject();
            outputJson.put("type", "wordCounts");
            ArrayNode dataJsonArray = outputJson.putArray("data");
            List<WordCount> results = JavaConversions.asJavaList(update.results());
            ObjectNode wordCountObject = Json.newObject();
            for (WordCount result : results) {
                wordCountObject.set(result.word, Json.toJson(result.count));
                dataJsonArray.add(wordCountObject);
            }
            out.write(outputJson);
        }
    }
}
