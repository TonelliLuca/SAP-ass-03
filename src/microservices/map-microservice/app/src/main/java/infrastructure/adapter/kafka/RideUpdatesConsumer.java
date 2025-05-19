package infrastructure.adapter.kafka;

import application.ports.RestMapServiceAPI;
import domain.model.EBike;
import domain.model.EBikeFactory;
import domain.model.EBikeState;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final RestMapServiceAPI mapService;
    private final GenericKafkaConsumer<JsonObject> rideConsumer;
    private final GenericKafkaConsumer<JsonObject> bikeConsumer;

    public RideUpdatesConsumer(RestMapServiceAPI mapService, String bootstrapServers) {
        this.mapService = mapService;

        this.rideConsumer = new GenericKafkaConsumer<>(
            bootstrapServers,
            "map-service-ride-group",
            "ride-events",
            JsonObject.class
        );

        this.bikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers,
            "map-service-ebike-group",
            "ebike-events",
            JsonObject.class
        );

        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        rideConsumer.start(this::processRideEvent);
        bikeConsumer.start(this::processBikeEvent);
        logger.info("RideUpdatesConsumer started - listening for ride and bike events");
    }

   private void processRideEvent(String key, JsonObject event) {
    try {
        logger.info("Received ride event: {}", event.encodePrettily());
        JsonObject payload = event.getJsonObject("payload");
        if (payload == null) {
            logger.error("Invalid ride event: missing payload");
            return;
        }
        JsonObject mapPayload = payload.getJsonObject("map");
        if (mapPayload == null) {
            logger.error("Invalid ride event: missing map in payload");
            return;
        }

        String status = mapPayload.getString("status");
        JsonObject rideWrapper = mapPayload.getJsonObject("ride");
        if (rideWrapper == null) {
            logger.error("Invalid ride event: missing ride data");
            return;
        }
        JsonObject rideData = rideWrapper.getJsonObject("map");
        if (rideData == null) {
            logger.error("Invalid ride event: missing ride map");
            return;
        }

        JsonObject bikeWrapper = rideData.getJsonObject("bike");
        JsonObject userWrapper = rideData.getJsonObject("user");
        if (bikeWrapper == null || userWrapper == null) {
            logger.error("Invalid ride event: missing bike or user data");
            return;
        }
        JsonObject bikeData = bikeWrapper.getJsonObject("map");
        JsonObject userData = userWrapper.getJsonObject("map");
        if (bikeData == null || userData == null) {
            logger.error("Invalid ride event: missing bike or user map");
            return;
        }

        String username = userData.getString("username");
        String bikeId = bikeData.getString("id");
        if (bikeId == null) {
            logger.error("Invalid ride event: missing bike identifier");
            return;
        }

        logger.debug("Processing ride event - status: {}, user: {}, bike: {}", status, username, bikeId);

        if ("START".equals(status)) {
            mapService.notifyStartRide(username, bikeId)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        logger.error("Failed to process ride start event: {}", error.getMessage());
                    } else {
                        logger.info("Successfully processed ride start for user {} and bike {}", username, bikeId);
                    }
                });
        } else if ("STOP".equals(status)) {
            mapService.notifyStopRide(username, bikeId)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        logger.error("Failed to process ride stop event: {}", error.getMessage());
                    } else {
                        logger.info("Successfully processed ride stop for user {} and bike {}", username, bikeId);
                    }
                });
        } else if ("INFO".equals(status)) {

           // Convert bikeData to EBike
            logger.info("Received INFO ride event  IGNORED");
       } else {
           logger.info("Received unknown ride status update - no action needed");
       }
    } catch (Exception e) {
        logger.error("Error processing ride event", e);
    }
}
    private void processBikeEvent(String key, JsonObject event) {
        try {
            logger.debug("Received bike event: {}", event.encodePrettily());
            String type = event.getString("type");

            if ("ebike_updated".equals(type)) {
                // Handle single bike update
                JsonObject bikeData = event.getJsonObject("payload");
                if (bikeData == null) {
                    logger.error("Invalid bike update: missing bike data");
                    return;
                }

                processBikeUpdate(bikeData);
            } else if ("ebikes_batch_updated".equals(type)) {
                // Handle batch update
                logger.info("Processing batch bike update");
                try {
                    if (event.getJsonObject("payload") != null) {
                        event.getJsonObject("payload").getJsonArray("list").forEach(bike -> {
                            if (bike instanceof JsonObject) {
                                processBikeUpdate((JsonObject) bike);
                            }
                        });
                    } else {
                        logger.error("Invalid batch update: payload is not a JsonArray");
                    }
                } catch (Exception e) {
                    logger.error("Error processing batch update", e);
                }
            } else {
                logger.warn("Unknown bike event type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Error processing bike event", e);
        }
    }

    private void processBikeUpdate(JsonObject bikeData) {
        try {
            logger.info("Received bike update: {}", bikeData.encodePrettily());
            // Accept both id and bikeName fields
            bikeData = bikeData.getJsonObject("map");
            String bikeName = bikeData.getString("id");
            if (bikeName == null) {
                logger.error("Invalid bike update: missing bike identifier");
                return;
            }

            float x = bikeData.getJsonObject("location").getJsonObject("map").getFloat("x", 0.0f);
            float y = bikeData.getJsonObject("location").getJsonObject("map").getFloat("y", 0.0f);
            int batteryLevel = bikeData.getInteger("batteryLevel", 100);
            EBikeState state = EBikeState.valueOf(bikeData.getString("state", "AVAILABLE"));
            EBike bike = EBikeFactory.getInstance().createEBike(bikeName, x, y, state, batteryLevel);

            mapService.updateEBike(bike)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        logger.error("Failed to update bike {}: {}", bikeName, error.getMessage());
                    } else {
                        logger.info("Successfully updated bike {}", bikeName);
                    }
                });
        } catch (Exception e) {
            logger.error("Error processing bike data: {}", e.getMessage());
        }
    }

    public void close() {
        rideConsumer.stop();
        bikeConsumer.stop();
        logger.info("RideUpdatesConsumer stopped");
    }
}