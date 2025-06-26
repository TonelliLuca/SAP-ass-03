package infrastructure.adapter.kafka;

import application.ports.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.BikeDockedEvent;
import domain.event.BikeReleasedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class StationConsumer {
    private static final Logger log = LoggerFactory.getLogger(StationConsumer.class);
    private final GenericKafkaConsumer<String> abikeEventConsumer;
    private final Service stationService;
    private final ObjectMapper mapper = new ObjectMapper();

    public StationConsumer(String bootstrapServers, Service stationService) {
        this.abikeEventConsumer = new GenericKafkaConsumer<>(bootstrapServers, "station-abike-group-"+ UUID.randomUUID(), "abike-events", String.class);
        this.stationService = stationService;
    }

    public void init() {
        abikeEventConsumer.start(this::processAbikeEvent);
        log.info("StationConsumer started - listening for abike events");
    }


    private void processAbikeEvent(String key, String value) {
        try {
            if ("ABikeArrivedToStation".equals(key)) {

                var root = mapper.readTree(value);
                String bikeId = root.get("bikeId").asText();
                String stationId = root.get("stationId").asText();
                BikeDockedEvent event = new BikeDockedEvent(bikeId, stationId);
                stationService.handleABikeArrivedToStation(event);
                log.info("Processed ABikeArrivedToStation event: {}", value);

            }else if("ABikeRequested".equals(key)) {
                var root = mapper.readTree(value);
                String bikeId = root.get("abikeId").asText();
                String stationId = root.get("stationId").asText();
                BikeReleasedEvent event = new  BikeReleasedEvent(bikeId, stationId);
                stationService.handleBikeReleased(event);

            }
        } catch (Exception e) {
            log.error("Error processing abike event: {}", e.getMessage(), e);
        }
    }
}