package infrastructure.adapter.kafka;

import application.ports.RestMapServiceAPI;
import domain.model.P2d;
import domain.model.Station;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StationUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(StationUpdatesConsumer.class);
    private final GenericKafkaConsumer<String> stationConsumer;
    private final RestMapServiceAPI mapService;

    public StationUpdatesConsumer(RestMapServiceAPI mapService, String bootstrapServers) {
        this.mapService = mapService;
        this.stationConsumer = new GenericKafkaConsumer<>(
            bootstrapServers,
            "map-service-station-group",
            "station-events",
            String.class
        );
        logger.info("StationUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        stationConsumer.start(this::processStationEvent);
        logger.info("StationUpdatesConsumer started - listening for station events");
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
}