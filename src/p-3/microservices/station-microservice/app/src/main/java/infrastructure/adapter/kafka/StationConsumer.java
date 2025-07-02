package infrastructure.adapter.kafka;

import application.ports.Service;
import domain.event.BikeDockedEvent;
import domain.event.BikeReleasedEvent;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class StationConsumer {
    private static final Logger log = LoggerFactory.getLogger(StationConsumer.class);
    private final AvroKafkaConsumer abikeEventConsumer;
    private final Service stationService;

    public StationConsumer(String bootstrapServers, String schemaRegistryUrl, Service stationService) {
        this.abikeEventConsumer = new AvroKafkaConsumer(
                bootstrapServers,
                schemaRegistryUrl,
                "station-abike-group-" + UUID.randomUUID(),
                "abike-events"
        );
        this.stationService = stationService;
    }

    public void init() {
        abikeEventConsumer.start(this::processAbikeEvent);
        log.info("StationConsumer started - listening for abike events");
    }

    private void processAbikeEvent(String key, GenericRecord envelopeRecord) {
        try {
            // Prendi l'evento dentro l'envelope (campo "event")
            GenericRecord eventRecord = (GenericRecord) envelopeRecord.get("event");
            if (eventRecord == null) {
                log.warn("Envelope Avro abike-events senza campo event, skippato");
                return;
            }
            String schemaName = eventRecord.getSchema().getName();

            if ("ABikeArrivedToStation".equals(schemaName)) {
                String bikeId = eventRecord.get("bikeId").toString();
                String stationId = eventRecord.get("stationId").toString();
                BikeDockedEvent event = new BikeDockedEvent(bikeId, stationId);
                stationService.handleABikeArrivedToStation(event);
                log.info("Processed ABikeArrivedToStation event: bikeId={}, stationId={}", bikeId, stationId);

            } else if ("ABikeRequested".equals(schemaName)) {
                String bikeId = eventRecord.get("abikeId").toString();
                String stationId = eventRecord.get("stationId").toString();
                BikeReleasedEvent event = new BikeReleasedEvent(bikeId, stationId);
                stationService.handleBikeReleased(event);
                log.info("Processed ABikeRequested event: abikeId={}, stationId={}", bikeId, stationId);

            } else {
                log.debug("Evento ignorato schema: {}", schemaName);
            }
        } catch (Exception e) {
            log.error("Error processing abike event: {}", e.getMessage(), e);
        }
    }
}
