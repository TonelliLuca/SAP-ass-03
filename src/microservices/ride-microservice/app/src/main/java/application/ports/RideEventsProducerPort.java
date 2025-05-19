package application.ports;

import domain.model.Ride;
import io.vertx.core.json.JsonObject;

public interface RideEventsProducerPort {
    void publishRideStart(String bikeId, String userId);
    void publishRideUpdate(JsonObject update);
    void publishRideEnd(String bikeId, String userId);
    void init();
}