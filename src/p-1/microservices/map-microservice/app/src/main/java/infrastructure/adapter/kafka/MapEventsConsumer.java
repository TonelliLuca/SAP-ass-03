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
    private final GenericKafkaConsumer<JsonObject> rideAbikeConsumer;
    private final GenericKafkaConsumer<JsonObject> rideEBikeConsumer;
    private final GenericKafkaConsumer<JsonObject> bikeConsumer;
    private final GenericKafkaConsumer<JsonObject> abikeConsumer;

    public MapEventsConsumer(RestMapServiceAPI mapService, String bootstrapServers) {
        this.mapService = mapService;
        this.stationConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-station-group", "station-events", String.class
        );
        this.rideAbikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-ride-abike-group", "ride-abike-events", JsonObject.class
        );
        this.rideEBikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-ride-ebike-group", "ride-ebike-events", JsonObject.class
        );
        this.bikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-ebike-group", "ebike-events", JsonObject.class
        );
        this.abikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-abike-group", "abike-events", JsonObject.class
        );
        logger.info("MapEventsConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        stationConsumer.start(this::processStationEvent);
        rideAbikeConsumer.start(this::processRideEvent);
        rideEBikeConsumer.start(this::processRideEvent);
        bikeConsumer.start(this::processEBikeEvent);
        abikeConsumer.start(this::processABikeEvent);
        logger.info("MapEventsConsumer started - listening for station, ride, and bike events");
    }

    private void processStationEvent(String key, String eventJson) {
        try {
            logger.info("Received station event: {} with key: {}", eventJson, key);
            Station station = null;
            if ("StationUpdateEvent".equals(key) || "StationRegisteredEvent".equals(key)) {
                JsonObject event = new JsonObject(eventJson);
                JsonObject stationObj = event.getJsonObject("station");
                if (stationObj != null) {
                    String stationId = stationObj.getString("id");
                    JsonObject location = stationObj.getJsonObject("location");
                    int capacity = stationObj.getInteger("capacity", 0);
                    int availableCapacity = stationObj.getInteger("availableCapacity", 0);
                    if (stationId != null && location != null) {
                        float x = location.getFloat("x", 0.0f);
                        float y = location.getFloat("y", 0.0f);
                        station = new Station(stationId, new P2d(x, y), capacity, availableCapacity);
                    }
                }
            }
            if (station != null) {
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
            String bikeType = bikeData.getString("type");
            if (bikeId == null) {
                logger.error("Invalid ride event: missing bike identifier");
                return;
            }

            logger.debug("Processing ride event - status: {}, user: {}, bike: {}, type: {}", status, username, bikeId, bikeType);

            switch (status) {
                case "START" -> {
                    if (!"abike".equalsIgnoreCase(bikeType)) {
                            mapService.notifyStartRide(username, bikeId, bikeType)
                            .whenComplete((result, error) -> {
                                if (error != null) {
                                    logger.error("Failed to process ride start event: {}", error.getMessage());
                                } else {
                                    logger.info("Successfully processed ride start for user {} and bike {}", username, bikeId);
                                }
                            });
                    } else {
                        logger.info("START event for abike {}: not from user", bikeId);
                        // Optionally, trigger other logic for abike here
                    }
                }
                case "STOP" -> {
                    if (!"abike".equalsIgnoreCase(bikeType)) {
                        mapService.notifyStopRide(username, bikeId,  bikeType)
                            .whenComplete((result, error) -> {
                                if (error != null) {
                                    logger.error("Failed to process ride stop event: {}", error.getMessage());
                                } else {
                                    logger.info("Successfully processed ride stop for user {} and bike {}", username, bikeId);
                                }
                            });
                    } else {
                        logger.info("STOP event for abike {}: not unbinding from user, waiting for return to station", bikeId);
                        // Optionally, trigger other logic for abike here
                    }
                }
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

    private void processABikeEvent(String key, JsonObject event) {
        try {
            JsonObject mapObj = event.getJsonObject("map");
            if (mapObj == null) {

                // Fallback: maybe the event itself is the ABike event
                mapObj = event;
            }
            String type = mapObj.getString("type");
            if ("ABikeRequested".equals(type)) {
                String abikeId = mapObj.getString("abikeId");
                String username = mapObj.getString("username");
                logger.info("Received ABikeRequested: abikeId={}, username={}", abikeId, username);
                mapService.notifyStartRide(username, abikeId, "abike");
            } else if ("ABikeUpdate".equals(type)) {
                JsonObject abikeData = mapObj.getJsonObject("abike");
                if (abikeData == null) {
                    logger.error("Invalid ABikeUpdate: missing abike data");
                    return;
                }
                String abikeId = abikeData.getString("id");
                JsonObject position = abikeData.getJsonObject("position");
                if (position == null) {
                    logger.error("Invalid ABikeUpdate: missing position for abikeId={}", abikeId);
                    return;
                }
                float x = position.getFloat("x", 0.0f);
                float y = position.getFloat("y", 0.0f);
                int batteryLevel = abikeData.getInteger("batteryLevel", 100);
                String stateStr = abikeData.getString("state", "AVAILABLE");
                BikeState state = BikeState.valueOf(stateStr);

                var abike = BikeFactory.getInstance().createABike(abikeId, x, y, state, batteryLevel);
                logger.info("Successfully updated ABike: {}", abikeId);
                mapService.updateBike(abike)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logger.error("Failed to update abike {}: {}", abikeId, error.getMessage());
                        } else {
                            logger.info("Successfully updated abike {}", abikeId);
                        }
                    });
            } else if ("ABikeArrivedToUser".equals(type)) {
                String abikeId = mapObj.getString("abikeId");
                String userId = mapObj.getString("userId");
                logger.info("Received ABikeArrivedToUser: abikeId={}, userId={}", abikeId, userId);
                mapService.notifyStartRide(userId, abikeId, "abike")
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                logger.error("Failed to assign abike {} to user {}: {}", abikeId, userId, error.getMessage());
                            } else {
                                logger.info("Successfully assigned abike {} to user {}", abikeId, userId);
                            }
                        });
                mapService.notifyABikeArrivedToUser(userId, abikeId);
            } else if ("ABikeCallComplete".equals(type)) {
                String abikeId = mapObj.getString("abikeId");
                String userId = mapObj.getString("userId");
                logger.info("Received ABikeCallComplete: abikeId={}, userId={}", abikeId, userId);
                mapService.notifyStopRide(userId, abikeId, "abike")
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                logger.error("Failed to process ABikeCallComplete stop ride: {}", error.getMessage());
                            } else {
                                logger.info("Successfully processed ABikeCallComplete stop ride for user {} and abike {}", userId, abikeId);
                            }
                        });
            }
        } catch (Exception e) {
            logger.error("Error processing ABike event", e);
        }
    }
}