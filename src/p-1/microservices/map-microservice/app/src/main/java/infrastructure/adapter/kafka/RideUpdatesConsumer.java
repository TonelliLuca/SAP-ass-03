package infrastructure.adapter.kafka;

import application.ports.RestMapServiceAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.model.EBike;
import domain.model.EBikeFactory;
import domain.model.EBikeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final RestMapServiceAPI mapService;
    private final GenericKafkaConsumer<String> rideConsumer;
    private final GenericKafkaConsumer<String> bikeConsumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RideUpdatesConsumer(RestMapServiceAPI mapService, String bootstrapServers) {
        this.mapService = mapService;
        this.rideConsumer = new GenericKafkaConsumer<>(bootstrapServers, "map-service-ride-group", "ride-events", String.class);
        this.bikeConsumer = new GenericKafkaConsumer<>(bootstrapServers, "map-service-ebike-group", "ebike-events", String.class);
        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        rideConsumer.start(this::processRideEvent);
        bikeConsumer.start(this::processBikeEvent);
        logger.info("RideUpdatesConsumer started - listening for ride and bike events");
    }

    private void processRideEvent(String key, String eventJson) {
        try {
            if (!"RideStartEvent".equals(key) && !"RideStopEvent".equals(key)) {
                logger.debug("Ignored ride event with key: {}", key);
                return;
            }
            JsonNode event = objectMapper.readTree(eventJson);
            String username = event.get("username").asText();
            String bikeId = event.get("bikeId").asText();
            if (username == null || bikeId == null) {
                logger.error("Invalid ride event: missing username or bikeId");
                return;
            }
            if ("RideStartEvent".equals(key)) {
                mapService.notifyStartRide(username, bikeId)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logger.error("Failed to process ride start event: {}", error.getMessage());
                        } else {
                            logger.info("Successfully processed ride start for user {} and bike {}", username, bikeId);
                        }
                    });
            } else if ("RideStopEvent".equals(key)) {
                mapService.notifyStopRide(username, bikeId)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logger.error("Failed to process ride stop event: {}", error.getMessage());
                        } else {
                            logger.info("Successfully processed ride stop for user {} and bike {}", username, bikeId);
                        }
                    });
            }
        } catch (Exception e) {
            logger.error("Error processing ride event", e);
        }
    }

    private void processBikeEvent(String key, String eventJson) {
        try {
            if (!"EBikeUpdateEvent".equals(key)) {
                logger.debug("Ignored bike event with key: {}", key);
                return;
            }
            JsonNode event = objectMapper.readTree(eventJson);
            JsonNode ebikeData = event.get("ebike");
            if (ebikeData == null) {
                logger.error("Invalid bike update: missing ebike data");
                return;
            }
            processBikeUpdate(ebikeData);
        } catch (Exception e) {
            logger.error("Error processing bike event", e);
        }
    }

    private void processBikeUpdate(JsonNode bikeData) {
        try {
            logger.info("Received bike update: {}", bikeData.toPrettyString());
            String bikeName = bikeData.get("id").asText();
            if (bikeName == null) {
                logger.error("Invalid bike update: missing bike identifier");
                return;
            }
            JsonNode location = bikeData.get("location");
            float x = location.get("x").floatValue();
            float y = location.get("y").floatValue();
            int batteryLevel = bikeData.has("batteryLevel") ? bikeData.get("batteryLevel").asInt() : 100;
            EBikeState state = EBikeState.valueOf(bikeData.has("state") ? bikeData.get("state").asText() : "AVAILABLE");
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
}