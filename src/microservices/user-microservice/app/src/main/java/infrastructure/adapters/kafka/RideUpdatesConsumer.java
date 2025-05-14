package infrastructure.adapters.kafka;

import application.ports.UserServiceAPI;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka adapter that consumes user updates from the ride-events topic
 * and applies them to the local user service.
 */
public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final UserServiceAPI userService;
    private final GenericKafkaConsumer<JsonObject> consumer;

    public RideUpdatesConsumer(UserServiceAPI ebikeService, String bootstrapServers) {
        this.userService = ebikeService;
        this.consumer = new GenericKafkaConsumer<>(
            bootstrapServers,
            "user-service-group",
            "ride-events",
            JsonObject.class
        );
        logger.info("RideUpadtesAdapter created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        consumer.start(this::processEBikeUpdate);
        logger.info("RideUpadtesAdapter started - listening for user updates from ride service");
    }

    private void processEBikeUpdate(String key, JsonObject event) {
        try {
            logger.info("Received user update event: {}", event.encodePrettily());

            // Extract user data from event payload
            JsonObject payload = event.getJsonObject("payload");
            if (payload == null || !payload.containsKey("ride")) {
                logger.error("Invalid ride update: missing payload or user object");
                return;
            }

            JsonObject userData = payload.getJsonObject("ride").getJsonObject("user");
            if (userData == null || !userData.containsKey("username")) {
                logger.error("Invalid user update: missing user data or id");
                return;
            }

            String username = userData.getString("username");
            logger.info("Processing update for user: {}", username);

            // Update the user in the local service
            userService.updateUser(userData)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed to update user: {}", throwable.getMessage());
                    } else if (result == null) {
                        logger.warn("User with id {} not found", username);
                    } else {
                        logger.info("Successfully updated user: {}", username);
                    }
                });
        } catch (Exception e) {
            logger.error("Error processing user update", e);
        }
    }

    public void close() {
        consumer.stop();
        logger.info("RideUpadtesAdapter stopped");
    }
}