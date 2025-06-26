package infrastructure.adapter.kafka;

import application.ports.RestMapServiceAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
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
    private final GenericKafkaConsumer<String> rideAbikeConsumer;
    private final GenericKafkaConsumer<String> rideEBikeConsumer;
    private final GenericKafkaConsumer<String> bikeConsumer;
    private final GenericKafkaConsumer<String> abikeConsumer;
    private final ObjectMapper mapper = new ObjectMapper();

    public MapEventsConsumer(RestMapServiceAPI mapService, String bootstrapServers) {
        this.mapService = mapService;
        this.stationConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-station-group", "station-events", String.class
        );
        this.rideAbikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-ride-abike-group", "ride-abike-events", String.class
        );
        this.rideEBikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-ride-ebike-group", "ride-ebike-events", String.class
        );
        this.bikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-ebike-group", "ebike-events", String.class
        );
        this.abikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "map-service-abike-group", "abike-events", String.class
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
            if ("StationUpdateEvent".equals(key) || "StationRegisteredEvent".equals(key)) {
                JsonNode eventNode = mapper.readTree(eventJson);
                JsonNode stationNode = eventNode.get("station");
                if (stationNode != NullNode.getInstance()) {
                    String stationId = stationNode.get("id").asText();
                    JsonNode locationNode = stationNode.get("location");
                    int capacity = stationNode.get("capacity").asInt();
                    int availableCapacity = stationNode.get("availableCapacity").asInt();

                    if (!stationId.isEmpty() && locationNode != NullNode.getInstance()) {
                        double x = locationNode.get("x").asDouble();
                        double y = locationNode.get("y").asDouble();
                        Station station = new Station(stationId, new P2d(x, y), capacity, availableCapacity);
                        mapService.updateStation(station);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error processing station event: {}", e.getMessage(), e);
        }
    }

    private void processRideEvent(String key, String event) {
        try {
            logger.info("Received ride event: {}", event);
            JsonNode eventNode = mapper.readTree(event);
            switch(key){
                case "RideStartEvent": {
                    String username = eventNode.get("username").asText();
                    String bikeId = eventNode.get("bikeId").asText();
                    String type = eventNode.get("type").asText();
                    if (type.equals("ebike")) {
                        mapService.notifyStartRide(username, bikeId, type)
                                .whenComplete((result, error) -> {
                                    if (error != null) {
                                        logger.error("Failed to process ride start event: {}", error.getMessage());
                                    } else {
                                        logger.info("Successfully processed ride start for user {} and bike {}", username, bikeId);
                                    }
                                });
                    }
                    break;
                }
                case "RideStopEvent": {
                    String username = eventNode.get("username").asText();
                    String bikeId = eventNode.get("bikeId").asText();
                    String type = eventNode.get("type").asText();
                    if (type.equals("ebike")) {
                        mapService.notifyStopRide(username, bikeId, type)
                                .whenComplete((result, error) -> {
                                    if (error != null) {
                                        logger.error("Failed to process ride stop event: {}", error.getMessage());
                                    } else {
                                        logger.info("Successfully processed ride stop for user {} and bike {}", username, bikeId);
                                    }
                                });
                    }
                    break;
                }

            }
        } catch (Exception e) {
            logger.error("Error processing ride event", e);
        }
    }

    private void processEBikeEvent(String key, String eventJson) {
        try {
            if (!"EBikeUpdateEvent".equals(key)) {
                logger.debug("Ignored bike event with key: {}", key);
                return;
            }
            logger.info("Received EBikeEvent: {}", eventJson);
            JsonNode event = mapper.readTree(eventJson);
            JsonNode ebikeData = event.get("ebike");
            if (ebikeData == null) {
                logger.error("Invalid bike update: missing ebike data");
                return;
            }
            processEBikeUpdate(ebikeData);
        } catch (Exception e) {
            logger.error("Error processing bike event", e);
        }
    }

    private void processEBikeUpdate(JsonNode bikeData) {
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
            BikeState state = BikeState.valueOf(bikeData.has("state") ? bikeData.get("state").asText() : "AVAILABLE");
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

    private void processABikeEvent(String key, String event) {
        try {

            JsonNode eventNode = mapper.readTree(event);
            switch(key){
                case "ABikeRequested": {
                    String abikeId = eventNode.get("abikeId").asText();
                    String username = eventNode.get("username").asText();
                    logger.info("Received ABikeRequested: abikeId={}, username={}", abikeId, username);
                    mapService.notifyStartRide(username, abikeId, "abike");
                    break;
                }
                case "ABikeUpdate": {
                    JsonNode abikeData = eventNode.get("abike");
                    String bikeId = abikeData.get("id").asText();
                    String state = abikeData.get("state").asText();
                    JsonNode location = abikeData.get("position");
                    float x = location.get("x").floatValue();
                    float y = location.get("y").floatValue();
                    int batteryLevel = abikeData.get("batteryLevel").asInt();
                    var abike = BikeFactory.getInstance().createABike(bikeId, x, y, BikeState.valueOf(state), batteryLevel);
                    mapService.updateBike(abike)
                            .whenComplete((result, error) -> {
                                if (error != null) {
                                    logger.error("Failed to update abike {}: {}", bikeId, error.getMessage());
                                } else {
                                    logger.info("Successfully updated abike {}", bikeId);
                                }
                            });
                    break;
                }
                case "ABikeArrivedToUser": {
                    String abikeId = eventNode.get("abikeId").asText();
                    String userId = eventNode.get("userId").asText();
                    mapService.notifyABikeArrivedToUser(userId, abikeId);
                    break;
                }
                case "ABikeCallComplete": {
                    String bikeId = eventNode.get("bikeId").asText();
                    String userId = eventNode.get("userId").asText();
                    mapService.notifyStopRide(userId, bikeId, "abike")
                            .whenComplete((result, error) -> {
                                if (error != null) {
                                    logger.error("Failed to process ABikeCallComplete stop ride: {}", error.getMessage());
                                } else {
                                    logger.info("Successfully processed ABikeCallComplete stop ride for user {} and abike {}", userId, bikeId);
                                }
                            });
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error processing ABike event", e);
        }
    }
}