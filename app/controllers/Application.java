package controllers;

import actors.AddAsWatcher;
import actors.UserActor;
import actors.UserWatcherActor;
import actors.WordCountActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;

public class Application extends Controller {

    private ActorRef userWatcherActor;

    @Inject
    public Application(@Named("userWatcherActor") ActorRef userWatcherActor) {
        System.out.println("Injecting userWatcherActor");
        this.userWatcherActor = userWatcherActor;
    }

    public Result index() {
        return ok(views.html.index.render());
    }

    public WebSocket<JsonNode> resultStream() {
        return new WebSocket<JsonNode>() {
            public void onReady(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {

                // Create userActor and tell the WordCountActor that it wants to be updated on new results
                final ActorRef userActor = Akka.system().actorOf(Props.create(UserActor.class, out));

                // Create the actor in charge of tracking which queries individual users are watching the result stream of
                AddAsWatcher addAsWatcher = new AddAsWatcher();
                System.out.println("Sending AddAsWatcher");
                userWatcherActor.tell(addAsWatcher, userActor);

                // send all WebSocket message to the UserActor
                in.onMessage(new F.Callback<JsonNode>() {
                    @Override
                    public void invoke(JsonNode jsonNode) throws Throwable {
                        System.out.println("Received message from the client");
                    }
                });

                // on close, tell the userActor to shutdown
                in.onClose(new F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {
                        Akka.system().stop(userActor);
                    }
                });
            }
        };
    }

}