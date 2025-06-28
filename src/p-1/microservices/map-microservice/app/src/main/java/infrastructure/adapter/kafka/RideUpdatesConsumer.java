package infrastructure.adapter.kafka;

import application.ports.RestMapServiceAPI;
import domain.model.EBike;
import domain.model.EBikeFactory;
import domain.model.EBikeState;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final RestMapServiceAPI mapService;
    private final AvroKafkaConsumer rideConsumer;
    private final AvroKafkaConsumer bikeConsumer;

    public RideUpdatesConsumer(RestMapServiceAPI mapService, String bootstrapServers, String schemaRegistryUrl) {
        this.mapService = mapService;
        this.rideConsumer = new AvroKafkaConsumer(bootstrapServers, schemaRegistryUrl, "map-service-ride-group", "ride-events");
        this.bikeConsumer = new AvroKafkaConsumer(bootstrapServers, schemaRegistryUrl, "map-service-ebike-group", "ebike-events");
        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        rideConsumer.start(this::processRideEvent);
        bikeConsumer.start(this::processBikeEvent);
        logger.info("RideUpdatesConsumer started - listening for ride and bike events");
    }

    private void processRideEvent(String key, GenericRecord unionRecord) {
        try {
            GenericRecord payload = (GenericRecord) unionRecord.get("payload");
            if (payload == null) {
                logger.debug("Ride union event missing payload, skip");
                return;
            }
            String schemaName = payload.getSchema().getName();
            if (!schemaName.equals("RideStartEventAvro") && !schemaName.equals("RideStopEventAvro")) {
                logger.debug("Ignored ride event schema: {}", schemaName);
                return;
            }
            String username = payload.get("username").toString();
            String bikeId = payload.get("bikeId").toString();
            if (username == null || bikeId == null) {
                logger.error("Invalid ride event: missing username or bikeId");
                return;
            }
            if ("RideStartEventAvro".equals(schemaName)) {
                mapService.notifyStartRide(username, bikeId)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                logger.error("Failed to process ride start event: {}", error.getMessage());
                            } else {
                                logger.info("Successfully processed ride start for user {} and bike {}", username, bikeId);
                            }
                        });
            } else if ("RideStopEventAvro".equals(schemaName)) {
                mapService.notifyStopRide(username, bikeId)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                logger.error("Failed to process ride stop event: {}", error.getMessage());
                            } else {
                                logger.info("Successfully processed ride stop for user {} and bike {}", username, bikeId);
                            }
                        });
            }
        } catch (Exception e) {
            logger.error("Error processing ride event", e);
        }
    }

    private void processBikeEvent(String key, GenericRecord event) {
        try {
            String schemaName = event.getSchema().getName();
            if (!"EBikeUpdateEventAvro".equals(schemaName)) {
                logger.debug("Ignored bike event schema: {}", schemaName);
                return;
            }
            GenericRecord ebikeData = (GenericRecord) event.get("ebike");
            if (ebikeData == null) {
                logger.error("Invalid bike update: missing ebike data");
                return;
            }
            processBikeUpdate(ebikeData);
        } catch (Exception e) {
            logger.error("Error processing bike event", e);
        }
    }

    private void processBikeUpdate(GenericRecord bikeData) {
        try {
            logger.info("Received bike update: {}", bikeData);
            String bikeName = bikeData.get("id").toString();
            double x = (double) bikeData.get("locationX");
            double y = (double) bikeData.get("locationY");
            int batteryLevel = (int) bikeData.get("batteryLevel");
            EBikeState state = EBikeState.valueOf(bikeData.get("state").toString());
            EBike bike = EBikeFactory.getInstance().createEBike(bikeName, (float)x, (float)y, state, batteryLevel);

            mapService.updateEBike(bike)
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
}
