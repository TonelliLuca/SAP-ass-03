package infrastructure.adapter.kafka;

import application.port.ABikeService;
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
    private final GenericKafkaConsumer<JsonObject> rideConsumer;
    private final ABikeService abikeService;

    public ABikeProjectionUpdatesConsumer(JsonObject config, ABikeService abikeService) {
        String bootstrapServers = config.getString("bootstrapServers", "kafka:29092");
        this.abikeService = abikeService;
        this.stationConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "abike-service-station-group", "station-events", String.class);
        this.rideConsumer = new GenericKafkaConsumer<>(
            bootstrapServers, "abike-service-ride-group", "ride-abike-events", JsonObject.class);
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
                abikeService.saveStationProjection(station);
            } else if ("StationUpdateEvent".equals(key)) {
                Station station = deserializeStation(eventJson);
                abikeService.updateStationProjection(station);
            }
        } catch (Exception e) {
            logger.error("Error processing station event: {}", e.getMessage(), e);
        }
    }

    private Station deserializeStation(String eventJson) {
        JsonObject event = new JsonObject(eventJson);
        JsonObject stationObj = event.getJsonObject("station");
        if (stationObj == null) {
            throw new IllegalArgumentException("Missing 'station' object in event");
        }
        String stationId = stationObj.getString("id");
        JsonObject location = stationObj.getJsonObject("location");
        int capacity = stationObj.getInteger("capacity", 0);

        HashSet<String> dockedBikes = new HashSet<>();
        if (stationObj.getJsonArray("dockedBikes") != null) {
            stationObj.getJsonArray("dockedBikes").forEach(bikeId -> {
                dockedBikes.add(bikeId.toString());
            });
        }

        return new Station(
            stationId,
            new P2d(location.getDouble("x"), location.getDouble("y")),
            dockedBikes,
            capacity
        );
    }



    private JsonObject unwrapMap(JsonObject obj) {
        while (obj != null && obj.containsKey("map")) {
            obj = obj.getJsonObject("map");
        }
        return obj;
    }

    private void processRideEvent(String key, JsonObject event) {
        try {
            logger.info("Received ride event: {}", event.encodePrettily());
            JsonObject root = unwrapMap(event);
            JsonObject payload = unwrapMap(root.getJsonObject("payload"));
            if (payload == null) {
                logger.error("Invalid ride event: missing payload");
                return;
            }

            JsonObject rideData = unwrapMap(payload.getJsonObject("ride"));
            if (rideData == null) {
                logger.error("Invalid ride event: missing ride data");
                return;
            }

            JsonObject bikeData = unwrapMap(rideData.getJsonObject("bike"));
            if (bikeData == null) {
                logger.error("Invalid ride event: missing bike data");
                return;
            }

            String bikeType = bikeData.getString("type");
            if (!"abike".equalsIgnoreCase(bikeType)) {
                logger.info("Skipping non-abike ride event (type={})", bikeType);
                return;
            }

            String abikeId = bikeData.getString("id", bikeData.getString("bikeName"));
            if (abikeId == null) {
                logger.error("Invalid ride event: missing abike identifier");
                return;
            }

            String status = payload.getString("status", "INFO");
            if ("STOP".equalsIgnoreCase(status)) {
                JsonObject userData = unwrapMap(rideData.getJsonObject("user"));
                String userId = userData != null ? userData.getString("username") : null;
                logger.info("STOP detected for abike {}: starting docking simulation for user {}", abikeId, userId);
                abikeService.completeCall(abikeId, userId);
                return;
            }

            int batteryLevel = bikeData.getInteger("batteryLevel", 100);
            String stateStr = bikeData.getString("state", "AVAILABLE");
            domain.model.ABikeState state;
            try {
                state = domain.model.ABikeState.valueOf(stateStr);
            } catch (Exception ex) {
                logger.error("Invalid abike state: {}", stateStr);
                return;
            }

            JsonObject pos = unwrapMap(
                bikeData.getJsonObject("position") != null
                    ? bikeData.getJsonObject("position")
                    : bikeData.getJsonObject("location")
            );
            if (pos == null) {
                logger.error("Invalid ride event: missing abike position");
                return;
            }
            double x = pos.getDouble("x", 0.0);
            double y = pos.getDouble("y", 0.0);

            ABike abike = new ABike(abikeId, new P2d(x, y), batteryLevel, state);

            abikeService.updateABike(abike)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed to update abike: {}", throwable.getMessage());
                    } else if (result == null) {
                        logger.warn("Abike with id {} not found", abikeId);
                    } else {
                        logger.info("Successfully updated abike: {}", abikeId);
                    }
                });
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