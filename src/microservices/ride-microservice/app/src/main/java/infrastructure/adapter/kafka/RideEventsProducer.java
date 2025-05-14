package infrastructure.adapter.kafka;

import application.ports.RideEventsProducerPort;
import domain.model.Ride;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RideEventsProducer implements RideEventsProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(RideEventsProducer.class);
    private final GenericKafkaProducer<JsonObject> rideProducer;

    public RideEventsProducer(String bootstrapServers) {
        this.rideProducer = new GenericKafkaProducer<>(bootstrapServers, "ride-events");
        logger.info("RideEventsProducer initialized with bootstrap servers: {}", bootstrapServers);
    }

    @Override
    public void publishRideStart(String bikeId, String userId) {
        logger.info("Publishing ride start event: bike={}, user={}", bikeId, userId);
        JsonObject payload = new JsonObject()
                .put("status", "START")
                .put("bike", new JsonObject().put("bikeName", bikeId))
                .put("user", new JsonObject().put("username", userId));

        publishEvent(payload, "ride_started");
    }

    @Override
    public void publishRideUpdate(Ride ride) {
        logger.info("Publishing ride update event: id={}", ride.getId());
        JsonObject bikeJson = new JsonObject()
                .put("id", ride.getEbike().getId())
                .put("state", ride.getEbike().getState().toString())
                .put("batteryLevel", ride.getEbike().getBatteryLevel())
                .put("location", new JsonObject()
                        .put("x", ride.getEbike().getLocation().x())
                        .put("y", ride.getEbike().getLocation().y()));

        JsonObject userJson = new JsonObject()
                .put("username", ride.getUser().getId())
                .put("credit", ride.getUser().getCredit());

        JsonObject rideJson = new JsonObject()
                .put("id", ride.getId())
                .put("bike", bikeJson)
                .put("user", userJson);

        JsonObject payload = new JsonObject()
                .put("status", "ON_GOING")
                .put("ride", rideJson);

        publishEvent(payload, "ride_updated");
    }

    @Override
    public void publishRideEnd(String bikeId, String userId) {
        logger.info("Publishing ride end event: bike={}, user={}", bikeId, userId);
        JsonObject payload = new JsonObject()
                .put("status", "STOP")
                .put("bike", new JsonObject().put("bikeName", bikeId))
                .put("user", new JsonObject().put("username", userId));

        publishEvent(payload, "ride_ended");
    }

    private void publishEvent(JsonObject payload, String eventType) {
        JsonObject event = new JsonObject()
                .put("type", eventType)
                .put("timestamp", System.currentTimeMillis())
                .put("payload", payload);

        rideProducer.send("ride-" + System.currentTimeMillis(), event);
        logger.debug("Published event: {}", event.encode());
    }

    public void close() {
        rideProducer.close();
        logger.info("RideEventsProducer closed");
    }
}