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
            bootstrapServers, "abike-service-ride-group", "ride-events", JsonObject.class);
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


    private void processRideEvent(String key, JsonObject event) {
        logger.info("Received ride event: {}", event.encodePrettily());
        JsonObject payload = event.getJsonObject("payload");
        if (payload == null) return;

        JsonObject abikeData = payload.getJsonObject("abike");
        if (abikeData != null) {
            ABike abike = mapJsonToABike(abikeData);

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