package infrastructure.adapter.kafka;

import application.ports.RestMapServiceAPI;
import domain.model.EBike;
import domain.model.BikeFactory;
import domain.model.BikeState;
import domain.model.P2d;
import domain.model.Station;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapEventsConsumer {
    private static final Logger logger = LoggerFactory.getLogger(MapEventsConsumer.class);

    private final RestMapServiceAPI mapService;
    private final GenericKafkaConsumer<String> stationConsumer;
    private final GenericKafkaConsumer<JsonObject> rideConsumer;
    private final GenericKafkaConsumer<JsonObject> bikeConsumer;

    public MapEventsConsumer(RestMapServiceAPI mapService, String bootstrapServers) {
        this.mapService = mapService;
        this.stationConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-station-group", "station-events", String.class
        );
        this.rideConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-ride-group", "ride-events", JsonObject.class
        );
        this.bikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-ebike-group", "ebike-events", JsonObject.class
        );
        logger.info("MapEventsConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        stationConsumer.start(this::processStationEvent);
        rideConsumer.start(this::processRideEvent);
        bikeConsumer.start(this::processEBikeEvent);
        logger.info("MapEventsConsumer started - listening for station, ride, and bike events");
    }

    private void processStationEvent(String key, String eventJson) {
        try {
            logger.info("Received station event: {}", eventJson);
            JsonObject event = new JsonObject(eventJson);
            String stationId = event.getString("stationId");
            JsonObject locationJson = event.getJsonObject("location");
            int capacity = event.getInteger("capacity", 0);
            int availableCapacity = event.getInteger("availableCapacity", 0);

            if (stationId != null && locationJson != null) {
                float x = locationJson.getFloat("x", 0.0f);
                float y = locationJson.getFloat("y", 0.0f);
                Station station = new Station(stationId, new P2d(x, y), capacity, availableCapacity);
                mapService.updateStation(station);
            } else {
                logger.error("Invalid station event: missing required fields");
            }
        } catch (Exception e) {
            logger.error("Error processing station event: {}", e.getMessage(), e);
        }
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

            switch (status) {
                case "START" -> mapService.notifyStartRide(username, bikeId)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                logger.error("Failed to process ride start event: {}", error.getMessage());
                            } else {
                                logger.info("Successfully processed ride start for user {} and bike {}", username, bikeId);
                            }
                        });
                case "STOP" -> mapService.notifyStopRide(username, bikeId)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                logger.error("Failed to process ride stop event: {}", error.getMessage());
                            } else {
                                logger.info("Successfully processed ride stop for user {} and bike {}", username, bikeId);
                            }
                        });
                case "INFO" ->
                    logger.info("Received INFO ride event  IGNORED");
                case null, default -> logger.info("Received unknown ride status update - no action needed");
            }
        } catch (Exception e) {
            logger.error("Error processing ride event", e);
        }
    }

    private void processEBikeEvent(String key, JsonObject event) {
        try {
            logger.debug("Received bike event: {}", event.encodePrettily());
            String type = event.getString("type");

            if ("ebike_updated".equals(type)) {
                JsonObject bikeData = event.getJsonObject("payload");
                if (bikeData == null) {
                    logger.error("Invalid bike update: missing bike data");
                    return;
                }
                processEBikeUpdate(bikeData);
            } else if ("ebikes_batch_updated".equals(type)) {
                logger.info("Processing batch bike update");
                try {
                    if (event.getJsonObject("payload") != null) {
                        event.getJsonObject("payload").getJsonArray("list").forEach(bike -> {
                            if (bike instanceof JsonObject) {
                                processEBikeUpdate((JsonObject) bike);
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

    private void processEBikeUpdate(JsonObject bikeData) {
        try {
            logger.info("Received bike update: {}", bikeData.encodePrettily());
            bikeData = bikeData.getJsonObject("map");
            String bikeName = bikeData.getString("id");
            if (bikeName == null) {
                logger.error("Invalid bike update: missing bike identifier");
                return;
            }

            float x = bikeData.getJsonObject("location").getJsonObject("map").getFloat("x", 0.0f);
            float y = bikeData.getJsonObject("location").getJsonObject("map").getFloat("y", 0.0f);
            int batteryLevel = bikeData.getInteger("batteryLevel", 100);
            BikeState state = BikeState.valueOf(bikeData.getString("state", "AVAILABLE"));
            EBike bike = BikeFactory.getInstance().createEBike(bikeName, x, y, state, batteryLevel);

            mapService.updateBike(bike)
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