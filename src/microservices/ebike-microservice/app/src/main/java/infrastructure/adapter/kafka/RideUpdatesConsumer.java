package infrastructure.adapter.kafka;

import application.ports.EBikeServiceAPI;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka adapter that consumes e-bike updates from the ride-events topic
 * and applies them to the local e-bike service.
 */
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
        logger.info("RideUpadtesAdapter created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        consumer.start(this::processEBikeUpdate);
        logger.info("RideUpadtesAdapter started - listening for e-bike updates from ride service");
    }

    private void processEBikeUpdate(String key, JsonObject event) {
        try {
            logger.info("Received e-bike update event: {}", event.encodePrettily());

            // Extract bike data from event payload
            JsonObject bikeData = event.getJsonObject("payload").getJsonObject("ride").getJsonObject("bike");
            if (bikeData == null || !bikeData.containsKey("id")) {
                logger.error("Invalid e-bike update: missing bike data or id");
                return;
            }

            String bikeId = bikeData.getString("id");
            logger.info("Processing update for bike: {}", bikeId);

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
        logger.info("RideUpadtesAdapter stopped");
    }
}