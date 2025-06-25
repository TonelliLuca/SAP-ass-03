package infrastructure.adapter.kafka;

import application.ports.UserServiceAPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.*;
import domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final UserServiceAPI userService;
    private final GenericKafkaConsumer<String> consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RideUpdatesConsumer(UserServiceAPI userService, String bootstrapServers) {
        this.userService = userService;
        this.consumer = new GenericKafkaConsumer<>(
            bootstrapServers,
            "user-service-group",
            "ride-events",
            String.class
        );
        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        consumer.start(this::processUserUpdate);
        logger.info("RideUpdatesConsumer started - listening for user updates from ride service");
    }

    private void processUserUpdate(String key, String eventJson) {
        try {
            if ("RideUpdateEvent".equals(key)) {
                // Deserialize RideUpdateEvent
                var rideUpdateNode = objectMapper.readTree(eventJson);
                String userId = rideUpdateNode.get("userId").asText();
                int userCredit = rideUpdateNode.get("userCredit").asInt();


                // Call userService.updateUser
                userService.updateUser(new RequestUserUpdateEvent(userId, userCredit));
                logger.info("Processed RideUpdateEvent and triggered UserUpdateEvent for userId={}", userId);
            }
        } catch (Exception e) {
            logger.error("Error processing user update", e);
        }
    }
}