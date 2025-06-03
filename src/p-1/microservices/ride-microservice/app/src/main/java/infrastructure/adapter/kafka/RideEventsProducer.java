package infrastructure.adapter.kafka;

import application.ports.EventPublisher;
import application.ports.RideEventsProducerPort;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideEventsProducer implements RideEventsProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(RideEventsProducer.class);
    private final GenericKafkaProducer<JsonObject> rideProducer;
    private final Vertx vertx;

    public RideEventsProducer(String bootstrapServers, Vertx vertx) {
        this.rideProducer = new GenericKafkaProducer<>(bootstrapServers, "ride-events");
        logger.info("RideEventsProducer initialized with bootstrap servers: {}", bootstrapServers);
        this.vertx = vertx;
    }

    @Override
    public void init(){
        vertx.eventBus().consumer(EventPublisher.RIDE_UPDATE, message -> {
            if (message.body() instanceof JsonObject update) {
                this.publishRideUpdate(update);
            }
        });

    }

    @Override
    public void publishRideStart(String bikeId, String userId, String bikeType) {
        logger.info("Publishing ride start event: bike={}, user={}", bikeId, userId);
        JsonObject bikeJson = new JsonObject()
                .put("id", bikeId)
                .put("type", bikeType);

        JsonObject userJson = new JsonObject()
                .put("username", userId);

        JsonObject rideJson = new JsonObject()
                .put("bike", bikeJson)
                .put("user", userJson);

        JsonObject payload = new JsonObject()
                .put("status", "START")
                .put("ride", rideJson);  // Add consistent ride wrapper

        publishEvent(payload, "ride_started");
    }

    @Override
    public void publishRideUpdate(JsonObject update) {
        logger.info("Publishing ride update event: {}", update.encodePrettily());
        publishEvent(update, "ride_updated");
    }

    @Override
    public void publishRideEnd(String bikeId, String userId, String bikeType) {
        logger.info("Publishing ride end event: bike={}, user={}", bikeId, userId);
        JsonObject bikeJson = new JsonObject()
                .put("id", bikeId)
                .put("type", bikeType);
        // Include both fields for compatibility

        JsonObject userJson = new JsonObject()
                .put("username", userId);

        JsonObject rideJson = new JsonObject()
                .put("bike", bikeJson)
                .put("user", userJson);

        JsonObject payload = new JsonObject()
                .put("status", "STOP")
                .put("ride", rideJson);  // Add consistent ride wrapper

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


}