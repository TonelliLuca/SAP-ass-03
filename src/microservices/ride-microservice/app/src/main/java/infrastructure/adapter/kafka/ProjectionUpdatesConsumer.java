package infrastructure.adapter.kafka;

import infrastructure.repository.LocalProjectionRepository;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ProjectionUpdatesConsumer.class);

    private final GenericKafkaConsumer<JsonObject> userConsumer;
    private final GenericKafkaConsumer<JsonObject> ebikeConsumer;
    private final LocalProjectionRepository projectionRepository;

    public ProjectionUpdatesConsumer(
            String bootstrapServers,
            LocalProjectionRepository projectionRepository) {

        this.projectionRepository = projectionRepository;

        this.userConsumer = new GenericKafkaConsumer<>(
            bootstrapServers,
            "ride-service-user-group",
            "user-events",
            JsonObject.class
        );

        this.ebikeConsumer = new GenericKafkaConsumer<>(
            bootstrapServers,
            "ride-service-ebike-group",
            "ebike-events",
            JsonObject.class
        );

        logger.info("ProjectionUpdatesConsumer created");
    }

    public void init() {
        userConsumer.start(this::processUserEvent);
        ebikeConsumer.start(this::processEbikeEvent);
        logger.info("ProjectionUpdatesConsumer started");
    }

    private void processUserEvent(String key, JsonObject event) {
        try {
            logger.info("Received user event: {}", event.encode());
            String type = event.getString("type");
            JsonObject payload = event.getJsonObject("payload");

            if ("user_updated".equals(type) && payload != null) {
                projectionRepository.updateUser(payload)
                    .exceptionally(ex -> {
                        logger.error("Error updating user projection: {}", ex.getMessage());
                        return null;
                    });
                logger.info("Processing user projection update: {}", payload.getString("username"));
            } else if ("users_batch_updated".equals(type)) {
                event.getJsonArray("payload").forEach(user -> {
                    if (user instanceof JsonObject) {
                        projectionRepository.updateUser((JsonObject) user)
                            .exceptionally(ex -> {
                                logger.error("Error updating user in batch: {}", ex.getMessage());
                                return null;
                            });
                    }
                });
                logger.info("Processing batch user update");
            }
        } catch (Exception e) {
            logger.error("Error processing user event", e);
        }
    }

    private void processEbikeEvent(String key, JsonObject event) {
        try {
            logger.info("Received ebike event: {}", event.encode());
            String type = event.getString("type");
            JsonObject payload = event.getJsonObject("payload");

            if ("ebike_updated".equals(type) && payload != null) {
                projectionRepository.updateEBike(payload)
                    .exceptionally(ex -> {
                        logger.error("Error updating ebike projection: {}", ex.getMessage());
                        return null;
                    });
                logger.info("Processing ebike projection update: {}", payload.getString("id"));
            } else if ("ebikes_batch_updated".equals(type)) {
                event.getJsonArray("payload").forEach(ebike -> {
                    if (ebike instanceof JsonObject) {
                        projectionRepository.updateEBike((JsonObject) ebike)
                            .exceptionally(ex -> {
                                logger.error("Error updating ebike in batch: {}", ex.getMessage());
                                return null;
                            });
                    }
                });
                logger.info("Processing batch ebike update");
            }
        } catch (Exception e) {
            logger.error("Error processing ebike event", e);
        }
    }

    public void close() {
        userConsumer.stop();
        ebikeConsumer.stop();
        logger.info("ProjectionUpdatesConsumer stopped");
    }
}