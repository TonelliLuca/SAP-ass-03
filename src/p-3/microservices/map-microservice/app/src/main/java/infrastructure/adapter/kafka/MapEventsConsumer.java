package infrastructure.adapter.kafka;

import application.ports.RestMapServiceAPI;
import domain.model.EBike;
import domain.model.BikeFactory;
import domain.model.BikeState;
import domain.model.P2d;
import domain.model.Station;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapEventsConsumer {
    private static final Logger logger = LoggerFactory.getLogger(MapEventsConsumer.class);

    private final RestMapServiceAPI mapService;
    private final AvroKafkaConsumer stationConsumer;
    private final AvroKafkaConsumer rideConsumer;
    private final AvroKafkaConsumer ebikeConsumer;
    private final AvroKafkaConsumer abikeConsumer;

    public MapEventsConsumer(RestMapServiceAPI mapService, String bootstrapServers, String schemaRegistryUrl) {
        this.mapService = mapService;
        this.stationConsumer = new AvroKafkaConsumer(
                bootstrapServers, schemaRegistryUrl, "map-service-station-group", "station-events"
        );
        this.rideConsumer = new AvroKafkaConsumer(
                bootstrapServers, schemaRegistryUrl, "map-service-ride-group", "ride-events"
        );
        this.ebikeConsumer = new AvroKafkaConsumer(
                bootstrapServers, schemaRegistryUrl, "map-service-ebike-group", "ebike-events"
        );
        this.abikeConsumer = new AvroKafkaConsumer(
                bootstrapServers, schemaRegistryUrl, "map-service-abike-group", "abike-events"
        );
        logger.info("MapEventsConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        stationConsumer.start(this::processStationEvent);
        rideConsumer.start(this::processRideEvent);
        ebikeConsumer.start(this::processEBikeEvent);
        abikeConsumer.start(this::processABikeEvent);
        logger.info("MapEventsConsumer started - listening for station, ride, and bike events");
    }

    // ----------- Station Events -----------
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

                // Leggi dockedBikes come Collection
                @SuppressWarnings("unchecked")
                java.util.Collection<CharSequence> dockedBikesCol = (java.util.Collection<CharSequence>) stationNode.get("dockedBikes");
                int dockedCount = dockedBikesCol != null ? dockedBikesCol.size() : 0;
                int availableCapacity = capacity - dockedCount;

                // Se serve, puoi anche passare la lista di docked bikes
                Station station = new Station(stationId, new P2d(x, y), capacity, availableCapacity);
                mapService.updateStation(station);

                logger.info("Processed station event ({}), stationId={}, dockedBikes={}, availableCapacity={}",
                        schemaName, stationId, dockedCount, availableCapacity);
            } else {
                logger.debug("Ignored station event schema: {}", schemaName);
            }
        } catch (Exception e) {
            logger.error("Error processing station event: {}", e.getMessage(), e);
        }
    }


    // ----------- Ride Events -----------
    private void processRideEvent(String key, GenericRecord envelopeRecord) {
        try {
            GenericRecord eventRecord = (GenericRecord) envelopeRecord.get("event");
            if (eventRecord == null) {
                logger.warn("Ride envelope senza campo event, skippato");
                return;
            }
            String schemaName = eventRecord.getSchema().getName();

            if ("RideStartEBikeEventAvro".equals(schemaName)) {
                String username = eventRecord.get("username").toString();
                String bikeId = eventRecord.get("bikeId").toString();
                mapService.notifyStartRide(username, bikeId, "ebike")
                        .whenComplete((result, error) -> logResult("ride start", username, bikeId, error));
            } else if ("RideStopEBikeEventAvro".equals(schemaName)) {
                String username = eventRecord.get("username").toString();
                String bikeId = eventRecord.get("bikeId").toString();
                mapService.notifyStopRide(username, bikeId, "ebike")
                        .whenComplete((result, error) -> logResult("ride stop", username, bikeId, error));
            } else {
                logger.debug("Ignored ride event schema: {}", schemaName);
            }
        } catch (Exception e) {
            logger.error("Error processing ride event", e);
        }
    }

    // ----------- EBike Events -----------
    private void processEBikeEvent(String key, GenericRecord eventRecord) {
        try {
            String schemaName = eventRecord.getSchema().getName();
            if (!"EBikeUpdateEventAvro".equals(schemaName)) {
                logger.debug("Ignored ebike event schema: {}", schemaName);
                return;
            }
            GenericRecord ebikeNode = (GenericRecord) eventRecord.get("ebike");
            String bikeId = ebikeNode.get("id").toString();
            GenericRecord locationNode = (GenericRecord) ebikeNode.get("location");
            float x = ((Double) locationNode.get("x")).floatValue();
            float y = ((Double) locationNode.get("y")).floatValue();
            int batteryLevel = (Integer) ebikeNode.get("batteryLevel");
            String state = ebikeNode.get("state").toString();
            EBike bike = BikeFactory.getInstance().createEBike(
                    bikeId, x, y, BikeState.valueOf(state), batteryLevel
            );
            mapService.updateBike(bike)
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            logger.error("Failed to update bike {}: {}", bikeId, error.getMessage());
                        } else {
                            logger.info("Successfully updated bike {}", bikeId);
                        }
                    });
        } catch (Exception e) {
            logger.error("Error processing ebike event", e);
        }
    }

    // ----------- ABike Events -----------
    private void processABikeEvent(String key, GenericRecord envelopeRecord) {
        try {
            GenericRecord eventRecord = (GenericRecord) envelopeRecord.get("event");
            if (eventRecord == null) {
                logger.warn("ABike envelope senza campo event, skippato");
                return;
            }
            String schemaName = eventRecord.getSchema().getName();

            switch (schemaName) {
                case "ABikeRequested": {
                    String abikeId = eventRecord.get("abikeId").toString();
                    String username = eventRecord.get("username").toString();
                    logger.info("Received ABikeRequested: abikeId={}, username={}", abikeId, username);
                    mapService.notifyStartRide(username, abikeId, "abike");
                    break;
                }
                case "ABikeUpdate": {
                    GenericRecord abikeData = (GenericRecord) eventRecord.get("abike");
                    String bikeId = abikeData.get("id").toString();
                    String state = abikeData.get("state").toString();
                    float x = ((Double) abikeData.get("x")).floatValue();
                    float y = ((Double) abikeData.get("y")).floatValue();
                    int batteryLevel = (Integer) abikeData.get("batteryLevel");
                    var abike = BikeFactory.getInstance().createABike(bikeId, x, y, BikeState.valueOf(state), batteryLevel);
                    mapService.updateBike(abike)
                            .whenComplete((result, error) -> {
                                if (error != null) {
                                    logger.error("Failed to update abike {}: {}", bikeId, error.getMessage());
                                } else {
                                    logger.info("Successfully updated abike {}", bikeId);
                                }
                            });
                    break;
                }
                case "ABikeArrivedToUser": {
                    String abikeId = eventRecord.get("abikeId").toString();
                    String userId = eventRecord.get("userId").toString();
                    mapService.notifyABikeArrivedToUser(userId, abikeId);
                    break;
                }
                case "ABikeCallComplete": {
                    String bikeId = eventRecord.get("bikeId").toString();
                    String userId = eventRecord.get("userId").toString();
                    mapService.notifyStopRide(userId, bikeId, "abike")
                            .whenComplete((result, error) -> {
                                if (error != null) {
                                    logger.error("Failed to process ABikeCallComplete stop ride: {}", error.getMessage());
                                } else {
                                    logger.info("Successfully processed ABikeCallComplete stop ride for user {} and abike {}", userId, bikeId);
                                }
                            });
                    break;
                }
                default:
                    logger.debug("Ignored abike event schema: {}", schemaName);
            }
        } catch (Exception e) {
            logger.error("Error processing ABike event", e);
        }
    }

    private void logResult(String action, String username, String bikeId, Throwable error) {
        if (error != null) {
            logger.error("Failed to process {}: user={} bike={} error={}", action, username, bikeId, error.getMessage());
        } else {
            logger.info("Successfully processed {} for user {} and bike {}", action, username, bikeId);
        }
    }
}
