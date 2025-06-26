package infrastructure.adapter.kafka;

import application.port.ABikeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.ABikeCallComplete;
import domain.event.ABikeUpdate;
import domain.event.RequestStationUpdate;
import domain.model.ABike;
import domain.model.ABikeState;
import domain.model.P2d;
import domain.model.Station;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

public class ABikeProjectionUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ABikeProjectionUpdatesConsumer.class);

    private final GenericKafkaConsumer<String> stationConsumer;
    private final GenericKafkaConsumer<String> rideConsumer;
    private final ABikeService abikeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ABikeProjectionUpdatesConsumer(JsonObject config, ABikeService abikeService) {
        String bootstrapServers = config.getString("bootstrapServers", "kafka:29092");
        this.abikeService = abikeService;
        this.stationConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "abike-service-station-group", "station-events", String.class);
        this.rideConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "abike-service-ride-group", "ride-abike-events", String.class);
    }

    public void init() {
        stationConsumer.start(this::processStationEvent);
        rideConsumer.start(this::processRideEvent);
        logger.info("ABikeProjectionUpdatesConsumer started");
    }

    private void processStationEvent(String key, String eventJson) {
        try {
            logger.info("Received station event: {} with key: {}", eventJson, key);
            if ("StationRegisteredEvent".equals(key)) {
                Station station = deserializeStation(eventJson);
                abikeService.saveStationProjection(new RequestStationUpdate(station));
            } else if ("StationUpdateEvent".equals(key)) {
                Station station = deserializeStation(eventJson);
                abikeService.updateStationProjection(new  RequestStationUpdate(station));
            }
        } catch (Exception e) {
            logger.error("Error processing station event: {}", e.getMessage(), e);
        }
    }

    private Station deserializeStation(String eventJson) {
        try {
            JsonNode node = objectMapper.readTree(eventJson);
            JsonNode stationNode = node.get("station");
            String stationId = stationNode.get("id").asText();
            JsonNode locationNode = stationNode.get("location");
            int capacity = stationNode.get("capacity").asInt();
            HashSet<String> dockedBikes = new HashSet<>();
            if (stationNode.has("dockedBikes") && stationNode.get("dockedBikes").isArray()) {
                for (JsonNode bikeIdNode : stationNode.get("dockedBikes")) {
                    dockedBikes.add(bikeIdNode.asText());
                }
            }
            double x = locationNode.get("x").asDouble();
            double y = locationNode.get("y").asDouble();
            P2d location = new P2d(x, y);
            return new Station(stationId, location, dockedBikes, capacity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    private void processRideEvent(String key, String event) {
        try {
            logger.warn("Received ride event: {} with key: {}", event, key);
            switch(key) {
            case "RideUpdateABikeEvent":
                JsonNode rideUpdateNode = objectMapper.readTree(event);
                String bikeId = rideUpdateNode.get("bikeId").asText();
                double bikeX = rideUpdateNode.get("bikeX").asDouble();
                double bikeY = rideUpdateNode.get("bikeY").asDouble();
                String bikeState = rideUpdateNode.get("bikeState").asText();
                int bikeBattery = rideUpdateNode.get("bikeBattery").asInt();

                ABikeUpdate update = new ABikeUpdate(new ABike(bikeId, new P2d(bikeX, bikeY), bikeBattery, ABikeState.valueOf(bikeState)));
                abikeService.updateABike(update)
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                logger.error("Failed to update abike: {}", throwable.getMessage());
                            } else if (result == null) {
                                logger.warn("Abike with id {} not found", bikeId);
                            } else {
                                logger.info("Successfully updated abike: {}", bikeId);
                            }
                        });
                break;

            case "RideStopEvent":
                JsonNode rideStopNode = objectMapper.readTree(event);
                String userId = rideStopNode.get("username").asText();
                String stopBikeId = rideStopNode.get("bikeId").asText();
                String type = rideStopNode.get("type").asText();
                if (type.equals("abike")) {
                    abikeService.completeCall(new ABikeCallComplete(stopBikeId, userId));
                }
                break;
            }


        } catch (Exception e) {
            logger.error("Error processing ride event", e);
        }
    }

    private ABike mapJsonToABike(JsonObject abikeData) {
        String id = abikeData.getString("id");
        int batteryLevel = abikeData.getInteger("batteryLevel");
        ABikeState state = ABikeState.valueOf(abikeData.getString("state"));
        JsonObject pos = abikeData.getJsonObject("position");
        P2d position = new P2d(pos.getDouble("x"), pos.getDouble("y"));
        String stationId = abikeData.getString("stationId");
        return new ABike(id, position, batteryLevel, state);
    }

    public void stop() {
        // Optionally implement stop logic for consumers
    }
}