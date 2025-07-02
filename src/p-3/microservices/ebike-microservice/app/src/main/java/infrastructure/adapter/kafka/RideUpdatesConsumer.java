package infrastructure.adapter.kafka;

import application.ports.EBikeServiceAPI;
import domain.event.RequestEBikeUpdateEvent;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final EBikeServiceAPI ebikeService;
    private final AvroKafkaConsumer consumer;

    public RideUpdatesConsumer(EBikeServiceAPI ebikeService, String bootstrapServers, String schemaRegistryUrl) {
        this.ebikeService = ebikeService;
        this.consumer = new AvroKafkaConsumer(
                bootstrapServers,
                schemaRegistryUrl,
                "ebike-service-group",
                "ride-events"
        );
        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        consumer.start(this::processRideEvent);
        logger.info("RideUpdatesConsumer started - listening for e-bike updates from ride service");
    }

    private void processRideEvent(String key, GenericRecord envelopeRecord) {
        try {
            GenericRecord eventRecord = (GenericRecord) envelopeRecord.get("event");
            if (eventRecord == null) {
                logger.warn("Envelope senza campo event, skippato");
                return;
            }
            String schemaName = eventRecord.getSchema().getName();
            if (!"RideUpdateEBikeEventAvro".equals(schemaName)) {
                logger.debug("Ignored ride event schema: {}", schemaName);
                return;
            }
            String bikeId = eventRecord.get("bikeId").toString();
            double bikeX = (Double) eventRecord.get("bikeX");
            double bikeY = (Double) eventRecord.get("bikeY");
            String bikeState = eventRecord.get("bikeState").toString();
            int bikeBattery = (Integer) eventRecord.get("bikeBattery");

            RequestEBikeUpdateEvent ebikeUpdateEvent = new RequestEBikeUpdateEvent(
                    bikeId, bikeX, bikeY, bikeState, bikeBattery
            );

            ebikeService.updateEBike(ebikeUpdateEvent)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            logger.error("Failed to update e-bike: {}", throwable.getMessage());
                        } else if (result == null) {
                            logger.warn("E-bike with id {} not found", bikeId);
                        } else {
                            logger.info("Successfully updated e-bike: {}", bikeId);
                        }
                    });
        } catch (Exception e) {
            logger.error("Error processing RideUpdateEBikeEventAvro", e);
        }
    }
}
