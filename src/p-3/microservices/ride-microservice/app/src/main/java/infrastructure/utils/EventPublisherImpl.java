package infrastructure.utils;

import application.ports.EventPublisher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class EventPublisherImpl implements EventPublisher {
     private final Vertx vertx;

    public EventPublisherImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public synchronized void publishRideUpdate(String id, double x, double y, String state, int batteryLevel, String username, int credit, String rideId, String bikeType){
        JsonObject bike = new JsonObject()
                .put("id", id)
                .put("state", state)
                .put("batteryLevel", batteryLevel)
                .put("type", bikeType)
                .put("location", new JsonObject()
                    .put("x", x)
                    .put("y", y));
        JsonObject user = new JsonObject()
                .put("username", username)
                .put("credit", credit);
        JsonObject rideJson = new JsonObject()
                .put("id", rideId)
                .put("bike", bike)
                .put("user", user);

        JsonObject payload = new JsonObject()
                .put("status", "INFO")
                .put("ride", rideJson);
        vertx.eventBus().publish(RIDE_UPDATE, payload);
    }

}
