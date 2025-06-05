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
            "ride-ebike-events",
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
            logger.info("Received ride event: {}", event.encodePrettily());
            JsonObject payload = event.getJsonObject("payload");
            if (payload == null) {
                logger.error("Invalid ride event: missing payload");
                return;
            }

            if (payload.containsKey("map")) {
                payload = payload.getJsonObject("map");
            }

            JsonObject rideData = payload.getJsonObject("ride");
            if (rideData == null) {
                logger.error("Invalid ride event: missing ride data");
                return;
            }

            JsonObject bikeData = rideData.getJsonObject("map").getJsonObject("bike");
            if (bikeData != null && bikeData.containsKey("map")) {
                bikeData = bikeData.getJsonObject("map");
            }

            if (bikeData == null) {
                logger.error("Invalid ride event: missing bike data");
                return;
            }

            String bikeType = bikeData.getString("type");
            if (!"ebike".equalsIgnoreCase(bikeType)) {
                logger.info("Skipping non-ebike ride event (type={})", bikeType);
                return;
            }

            String bikeId = bikeData.getString("id", bikeData.getString("bikeName"));
            if (bikeId == null) {
                logger.error("Invalid ride event: missing bike identifier");
                return;
            }

            logger.info("Processing update for e-bike: {}", bikeId);

            if (!bikeData.containsKey("id")) {
                bikeData.put("id", bikeId);
            }

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
            logger.error("Error processing ride event", e);
        }
    }


}