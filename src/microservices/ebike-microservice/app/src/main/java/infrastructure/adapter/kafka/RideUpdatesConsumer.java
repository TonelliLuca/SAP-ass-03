package infrastructure.adapter.kafka;

import application.ports.EBikeServiceAPI;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final EBikeServiceAPI ebikeService;
    private final GenericKafkaConsumer<JsonObject> consumer;

    public RideUpdatesConsumer(EBikeServiceAPI ebikeService, String bootstrapServers) {
        this.ebikeService = ebikeService;
        this.consumer = new GenericKafkaConsumer<>(
            bootstrapServers,
            "ebike-service-group",
            "ride-events",
            JsonObject.class
        );
        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        consumer.start(this::processRideEvent);
        logger.info("RideUpdatesConsumer started - listening for e-bike updates from ride service");
    }

    private void processRideEvent(String key, JsonObject event) {
        try {
            logger.info("Received e-bike update event: {}", event.encodePrettily());
            String type = event.getString("type");
            JsonObject payload = event.getJsonObject("payload");

            if (payload == null) {
                logger.error("Invalid e-bike update: missing payload");
                return;
            }

            // Get ride data from the standardized format
            JsonObject rideData = payload.getJsonObject("ride");
            if (rideData == null) {
                logger.error("Invalid e-bike update: missing ride data");
                return;
            }

            JsonObject bikeData = rideData.getJsonObject("bike");
            if (bikeData == null) {
                logger.error("Invalid e-bike update: missing bike data");
                return;
            }

            // Accept both id and bikeName fields
            String bikeId = bikeData.getString("id", bikeData.getString("bikeName"));
            if (bikeId == null) {
                logger.error("Invalid e-bike update: missing bike identifier");
                return;
            }

            logger.info("Processing update for bike: {}", bikeId);

            // Ensure id field is present for service processing
            if (!bikeData.containsKey("id")) {
                bikeData.put("id", bikeId);
            }

            // Update the e-bike in the local service
            ebikeService.updateEBike(bikeData)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed to update e-bike: {}", throwable.getMessage());
                    } else if (result == null) {
                        logger.warn("E-bike with id {} not found", bikeId);
                    } else {
                        logger.info("Successfully updated e-bike: {}", bikeId);
                    }
                });
        } catch (Exception e) {
            logger.error("Error processing e-bike update", e);
        }
    }

    public void close() {
        consumer.stop();
        logger.info("RideUpdatesConsumer stopped");
    }
}