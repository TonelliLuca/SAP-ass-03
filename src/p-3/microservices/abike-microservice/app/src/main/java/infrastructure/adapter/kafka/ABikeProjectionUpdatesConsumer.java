package infrastructure.adapter.kafka;

import application.port.ABikeService;
import domain.event.ABikeCallComplete;
import domain.event.ABikeUpdate;
import domain.event.RequestStationUpdate;
import domain.model.ABike;
import domain.model.ABikeState;
import domain.model.P2d;
import domain.model.Station;
import io.vertx.core.json.JsonObject;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.UUID;

public class ABikeProjectionUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ABikeProjectionUpdatesConsumer.class);

    private final AvroKafkaConsumer stationConsumer;
    private final AvroKafkaConsumer rideConsumer;
    private final ABikeService abikeService;

    public ABikeProjectionUpdatesConsumer(String bootstrapServers, String schemaRegistryUrl, ABikeService abikeService) {
        this.abikeService = abikeService;
        this.stationConsumer = new AvroKafkaConsumer(
                bootstrapServers, schemaRegistryUrl, "abike-service-station-group-" + UUID.randomUUID(), "station-events");
        this.rideConsumer = new AvroKafkaConsumer(
                bootstrapServers, schemaRegistryUrl, "abike-service-ride-group-" + UUID.randomUUID(), "ride-events");
    }

    public void init() {
        stationConsumer.start(this::processStationEvent);
        rideConsumer.start(this::processRideEvent);
        logger.info("ABikeProjectionUpdatesConsumer started (Avro mode)");
    }

    private void processStationEvent(String key, GenericRecord envelopeRecord) {
        try {
            GenericRecord eventRecord = (GenericRecord) envelopeRecord.get("event");
            if (eventRecord == null) {
                logger.warn("Station envelope senza campo event, skippato");
                return;
            }
            String schemaName = eventRecord.getSchema().getName();

            if ("StationRegisteredEventAvro".equals(schemaName) || "StationUpdateEventAvro".equals(schemaName)) {
                GenericRecord stationNode = (GenericRecord) eventRecord.get("station");
                String stationId = stationNode.get("id").toString();
                GenericRecord locationNode = (GenericRecord) stationNode.get("location");
                double x = (Double) locationNode.get("x");
                double y = (Double) locationNode.get("y");
                int capacity = (Integer) stationNode.get("capacity");

                // Leggi dockedBikes
                @SuppressWarnings("unchecked")
                java.util.Collection<CharSequence> dockedBikesCol = (java.util.Collection<CharSequence>) stationNode.get("dockedBikes");
                HashSet<String> dockedBikes = new HashSet<>();
                if (dockedBikesCol != null) {
                    dockedBikesCol.forEach(c -> dockedBikes.add(c.toString()));
                }

                Station station = new Station(stationId, new P2d(x, y), dockedBikes, capacity);
                if ("StationRegisteredEventAvro".equals(schemaName)) {
                    abikeService.saveStationProjection(new RequestStationUpdate(station));
                } else {
                    abikeService.updateStationProjection(new RequestStationUpdate(station));
                }
                logger.info("Processed {} for stationId={}", schemaName, stationId);
            } else {
                logger.debug("Ignored station event schema: {}", schemaName);
            }
        } catch (Exception e) {
            logger.error("Error processing station event: {}", e.getMessage(), e);
        }
    }

    private void processRideEvent(String key, GenericRecord envelopeRecord) {
        try {
            GenericRecord eventRecord = (GenericRecord) envelopeRecord.get("event");
            if (eventRecord == null) {
                logger.warn("Ride envelope senza campo event, skippato");
                return;
            }
            String schemaName = eventRecord.getSchema().getName();

            switch (schemaName) {
                case "RideUpdateABikeEventAvro": {
                    String bikeId = eventRecord.get("bikeId").toString();
                    double bikeX = (Double) eventRecord.get("bikeX");
                    double bikeY = (Double) eventRecord.get("bikeY");
                    String bikeState = eventRecord.get("bikeState").toString();
                    int bikeBattery = (Integer) eventRecord.get("bikeBattery");
                    ABikeUpdate update = new ABikeUpdate(new ABike(
                            bikeId,
                            new P2d(bikeX, bikeY),
                            bikeBattery,
                            ABikeState.valueOf(bikeState)
                    ));
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
                }
                case "RideStopABikeEventAvro": {
                    String userId = eventRecord.get("username").toString();
                    String stopBikeId = eventRecord.get("bikeId").toString();
                    abikeService.completeCall(new ABikeCallComplete(stopBikeId, userId));
                    break;
                }
                default:
                    logger.debug("Ignored ride event schema: {}", schemaName);
            }
        } catch (Exception e) {
            logger.error("Error processing ride event", e);
        }
    }

    public void stop() {
        stationConsumer.stop();
        rideConsumer.stop();
    }
}
