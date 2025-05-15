package infrastructure.adapter.kafka;

import application.ports.UserServiceAPI;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final UserServiceAPI userService;
    private final GenericKafkaConsumer<JsonObject> consumer;

    public RideUpdatesConsumer(UserServiceAPI userService, String bootstrapServers) {
        this.userService = userService;
        this.consumer = new GenericKafkaConsumer<>(
            bootstrapServers,
            "user-service-group",
            "ride-events",
            JsonObject.class
        );
        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        consumer.start(this::processUserUpdate);
        logger.info("RideUpdatesConsumer started - listening for user updates from ride service");
    }

    private void processUserUpdate(String key, JsonObject event) {
        try {
            logger.info("Received user update event: {}", event.encodePrettily());
            String type = event.getString("type");
            JsonObject payload = event.getJsonObject("payload");

            if (payload == null) {
                logger.error("Invalid ride update: missing payload");
                return;
            }

            // Get ride data from the standardized format
            JsonObject rideData = payload.getJsonObject("ride");
            if (rideData == null) {
                logger.error("Invalid ride update: missing ride data");
                return;
            }

            JsonObject userData = rideData.getJsonObject("user");
            if (userData == null) {
                logger.error("Invalid user update: missing user data");
                return;
            }

            String username = userData.getString("username");
            if (username == null) {
                logger.error("Invalid user update: missing username");
                return;
            }

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
        logger.info("RideUpdatesConsumer stopped");
    }
}