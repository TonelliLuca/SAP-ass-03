package infrastructure.adapter.kafka;

import application.ports.ProjectionRepositoryPort;
import application.ports.RestRideServiceAPI;
import domain.event.ABikeUpdateEvent;
import domain.event.EBikeUpdateEvent;
import domain.event.RideStartEvent;
import domain.event.UserUpdateEvent;
import domain.model.BikeState;
import domain.model.P2d;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ProjectionUpdatesConsumer.class);

    private final AvroKafkaConsumer userConsumer;
    private final AvroKafkaConsumer ebikeConsumer;
    private final AvroKafkaConsumer abikeConsumer;
    private final ProjectionRepositoryPort projectionRepository;
    private final RestRideServiceAPI service;

    public ProjectionUpdatesConsumer(
            String bootstrapServers,
            String schemaRegistryUrl,
            ProjectionRepositoryPort projectionRepository,
            RestRideServiceAPI service) {
        this.service = service;
        this.projectionRepository = projectionRepository;

        this.userConsumer = new AvroKafkaConsumer(
                bootstrapServers,
                schemaRegistryUrl,
                "ride-service-user-group",
                "user-events"
        );
        this.ebikeConsumer = new AvroKafkaConsumer(
                bootstrapServers,
                schemaRegistryUrl,
                "ride-service-ebike-group",
                "ebike-events"
        );
        this.abikeConsumer = new AvroKafkaConsumer(
                bootstrapServers,
                schemaRegistryUrl,
                "ride-service-abike-group",
                "abike-events"
        );

        logger.info("ProjectionUpdatesConsumer created");
    }

    public void init() {
        userConsumer.start(this::processUserEvent);
        ebikeConsumer.start(this::processEbikeEvent);
        abikeConsumer.start(this::processAbikeEvent);
        logger.info("ProjectionUpdatesConsumer started");
    }

    private void processUserEvent(String key, GenericRecord eventRecord) {
        try {
            String schemaName = eventRecord.getSchema().getName();
            if (!"UserUpdateEventAvro".equals(schemaName)) {
                logger.debug("Ignored user event schema: {}", schemaName);
                return;
            }

            logger.info("Received user event: {}", eventRecord);

            String id = eventRecord.get("id").toString();
            String timestamp = eventRecord.get("timestamp").toString();

            GenericRecord userNode = (GenericRecord) eventRecord.get("user");
            String username = userNode.get("username").toString();
            int credit = (Integer) userNode.get("credit");

            UserUpdateEvent flatEvent = new UserUpdateEvent(id, username, credit, timestamp);

            projectionRepository.appendUserEvent(flatEvent)
                    .exceptionally(ex -> {
                        logger.error("Error appending FLAT user update event: {}", ex.getMessage());
                        return null;
                    });

            logger.info("Appended FLAT UserUpdateEvent for user: {}", flatEvent.username());

        } catch (Exception e) {
            logger.error("Error processing user event", e);
        }
    }

    private void processEbikeEvent(String key, GenericRecord eventRecord) {
        try {
            String schemaName = eventRecord.getSchema().getName();
            if (!"EBikeUpdateEventAvro".equals(schemaName)) {
                logger.debug("Ignored ebike event schema: {}", schemaName);
                return;
            }

            logger.info("Received ebike event: {}", eventRecord);

            String id = eventRecord.get("id").toString();
            String timestamp = eventRecord.get("timestamp").toString();

            GenericRecord ebikeNode = (GenericRecord) eventRecord.get("ebike");
            String bikeId = ebikeNode.get("id").toString();
            String stateStr = ebikeNode.get("state").toString();
            int batteryLevel = (Integer) ebikeNode.get("batteryLevel");

            GenericRecord locationNode = (GenericRecord) ebikeNode.get("location");
            double x = (Double) locationNode.get("x");
            double y = (Double) locationNode.get("y");

            EBikeUpdateEvent flatEvent = new EBikeUpdateEvent(
                    id,
                    bikeId,
                    BikeState.valueOf(stateStr),
                    new P2d(x, y),
                    batteryLevel,
                    timestamp
            );

            projectionRepository.appendEBikeEvent(flatEvent)
                    .exceptionally(ex -> {
                        logger.error("Error appending FLAT ebike update event: {}", ex.getMessage());
                        return null;
                    });

            logger.info("Appended FLAT EBikeUpdateEvent for bike: {}", flatEvent.bikeId());

        } catch (Exception e) {
            logger.error("Error processing ebike event", e);
        }
    }

    private void processAbikeEvent(String key, GenericRecord envelopeRecord) {
        try {
            // Estrai il campo union "event" dall'envelope
            GenericRecord eventRecord = (GenericRecord) envelopeRecord.get("event");
            if (eventRecord == null) {
                logger.warn("Envelope senza campo event, skippato");
                return;
            }

            // Discriminatore: il nome dello schema del tipo effettivo dentro la union
            String schemaName = eventRecord.getSchema().getName();

            switch (schemaName) {
                case "ABikeUpdate":
                    logger.info("Received abike event: {}", eventRecord);
                    String id = eventRecord.get("id").toString();
                    String timestamp = eventRecord.get("timestamp").toString();

                    GenericRecord abikeNode = (GenericRecord) eventRecord.get("abike");
                    String bikeId = abikeNode.get("id").toString();
                    String stateStr = abikeNode.get("state").toString();
                    int batteryLevel = (Integer) abikeNode.get("batteryLevel");
                    double x = (Double) abikeNode.get("x");
                    double y = (Double) abikeNode.get("y");

                    ABikeUpdateEvent flatEvent = new ABikeUpdateEvent(
                            id,
                            bikeId,
                            BikeState.valueOf(stateStr),
                            new P2d(x, y),
                            batteryLevel,
                            timestamp
                    );

                    projectionRepository.appendABikeEvent(flatEvent)
                            .exceptionally(ex -> {
                                logger.error("Error appending FLAT abike update event: {}", ex.getMessage());
                                return null;
                            });

                    logger.info("Appended FLAT ABikeUpdateEvent for bike: {}", flatEvent.bikeId());
                    break;

                case "ABikeArrivedToUser":
                    logger.info("Received abike arrived to user: {}", eventRecord);
                    String abikeId = eventRecord.get("abikeId").toString();
                    String userId = eventRecord.get("userId").toString();
                    String t = eventRecord.get("timestamp").toString();
                    service.startRide(new RideStartEvent(userId, abikeId, "abike"))
                            .exceptionally(ex -> {
                                logger.error("Error starting abike ride: {}", ex.getMessage());
                                return null;
                            });
                    break;

                default:
                    logger.debug("Ignored abike event schema: {}", schemaName);
            }
        } catch (Exception e) {
            logger.error("Error processing abike event", e);
        }
    }
}
