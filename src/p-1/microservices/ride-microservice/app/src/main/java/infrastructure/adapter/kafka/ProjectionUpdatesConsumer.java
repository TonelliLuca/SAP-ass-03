package infrastructure.adapter.kafka;

import application.ports.ProjectionRepositoryPort;
import domain.event.EBikeUpdateEvent;
import domain.event.UserUpdateEvent;
import domain.model.EBikeState;
import domain.model.P2d;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ProjectionUpdatesConsumer.class);

    private final AvroKafkaConsumer userConsumer;
    private final AvroKafkaConsumer ebikeConsumer;
    private final ProjectionRepositoryPort projectionRepository;

    public ProjectionUpdatesConsumer(
            String bootstrapServers,
            String schemaRegistryUrl,
            ProjectionRepositoryPort projectionRepository) {

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

        logger.info("ProjectionUpdatesConsumer created");
    }

    public void init() {
        userConsumer.start(this::processUserEvent);
        ebikeConsumer.start(this::processEbikeEvent);
        logger.info("ProjectionUpdatesConsumer started");
    }

    private void processUserEvent(String key, GenericRecord event) {
        String schemaName = event.getSchema().getName();
        if (!"UserUpdateEventAvro".equals(schemaName)) {
            logger.debug("Ignored user event with schema: {}", schemaName);
            return;
        }
        try {
            logger.info("Received user event: {}", event);

            // Flat extraction
            String id = event.get("id").toString();
            String timestamp = event.get("timestamp").toString();

            GenericRecord userNode = (GenericRecord) event.get("user");
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

    private void processEbikeEvent(String key, GenericRecord event) {
        String schemaName = event.getSchema().getName();
        if (!"EBikeUpdateEventAvro".equals(schemaName)) {
            logger.debug("Ignored ebike event with schema: {}", schemaName);
            return;
        }
        try {
            logger.info("Received ebike event: {}", event);

            String id = event.get("id").toString();
            String timestamp = event.get("timestamp").toString();

            GenericRecord ebikeNode = (GenericRecord) event.get("ebike");
            String bikeId = ebikeNode.get("id").toString();
            String stateStr = ebikeNode.get("state").toString();
            int batteryLevel = (Integer) ebikeNode.get("batteryLevel");
            double x = (Double) ebikeNode.get("locationX");
            double y = (Double) ebikeNode.get("locationY");

            EBikeUpdateEvent flatEvent = new EBikeUpdateEvent(
                    id,
                    bikeId,
                    EBikeState.valueOf(stateStr),
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
}
