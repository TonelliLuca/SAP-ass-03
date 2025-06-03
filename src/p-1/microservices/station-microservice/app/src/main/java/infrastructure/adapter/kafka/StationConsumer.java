package infrastructure.adapter.kafka;

import application.ports.DomainEventPublisher;
import application.ports.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.events.BikeDockedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StationConsumer {
    private static final Logger log = LoggerFactory.getLogger(StationConsumer.class);
    private final GenericKafkaConsumer<String> abikeEventConsumer;
    private final Service stationService;
    private final ObjectMapper mapper = new ObjectMapper();

    public StationConsumer(String bootstrapServers, Service stationService) {
        this.abikeEventConsumer = new GenericKafkaConsumer<>(bootstrapServers, "station-abike-group", "abike-events", String.class);
        this.stationService = stationService;
    }

    public void init() {
        abikeEventConsumer.start(this::processAbikeEvent);
        log.info("StationConsumer started - listening for abike events");
    }

    private void processAbikeEvent(String key, String value) {
        try {
            if ("ABikeArrivedToStation".equals(key)) {
                BikeDockedEvent event = mapper.readValue(value, BikeDockedEvent.class);
                stationService.handleABikeArrivedToStation(event);
                log.info("Processed ABikeArrivedToStation event: {}", value);
            }
        } catch (Exception e) {
            log.error("Error processing abike event: {}", e.getMessage(), e);
        }
    }
}