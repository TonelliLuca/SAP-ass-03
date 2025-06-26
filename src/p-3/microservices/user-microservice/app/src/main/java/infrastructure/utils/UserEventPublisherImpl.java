package infrastructure.utils;

import application.ports.UserEventPublisher;
import domain.event.Event;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;


public class UserEventPublisherImpl implements  UserEventPublisher{
    private final Vertx vertx;

    public UserEventPublisherImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void publishUserUpdate(String username, Event event) {
        JsonObject eventJson = JsonObject.mapFrom(event);
        vertx.eventBus().publish(username, eventJson.encode());
    }

    @Override
    public void publishAllUsersUpdates(Event event) {
        JsonObject eventJson = JsonObject.mapFrom(event);
        vertx.eventBus().publish("users.update", eventJson.encode());
    }

}
