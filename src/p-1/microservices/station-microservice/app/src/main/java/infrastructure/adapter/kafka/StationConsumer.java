package infrastructure.adapter.kafka;

import application.ports.DomainEventPublisher;
import application.ports.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.events.BikeDockedEvent;
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
                // Parse the JSON and extract the "map" node
                var root = mapper.readTree(value);
                var mapNode = root.get("map");
                if (mapNode != null) {
                    // Map the "map" node to your event class
                    BikeDockedEvent event = mapper.treeToValue(mapNode, BikeDockedEvent.class);
                    stationService.handleABikeArrivedToStation(event);
                    log.info("Processed ABikeArrivedToStation event: {}", value);
                } else {
                    log.warn("No 'map' field found in event: {}", value);
                }
            }
        } catch (Exception e) {
            log.error("Error processing abike event: {}", e.getMessage(), e);
        }
    }
}