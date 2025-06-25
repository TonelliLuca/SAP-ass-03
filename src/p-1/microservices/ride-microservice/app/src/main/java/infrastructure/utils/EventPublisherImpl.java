package infrastructure.utils;

import application.ports.EventPublisher;
import domain.event.Event;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class EventPublisherImpl implements EventPublisher {
     private final Vertx vertx;

    public EventPublisherImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void publishUpdate(Event event) {
        vertx.eventBus().publish(RIDE_UPDATE, event);
    }
}
