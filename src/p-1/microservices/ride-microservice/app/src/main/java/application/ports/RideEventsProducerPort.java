package application.ports;


import io.vertx.core.json.JsonObject;

public interface RideEventsProducerPort {
    void publishRideStart(String bikeId, String userId, String bikeType);
    void publishRideUpdate(JsonObject update);
    void publishRideEnd(String bikeId, String userId, String bikeType);
    void init();
}