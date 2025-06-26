package infrastructure.adapter.kafka;

import application.ports.UserServiceAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.*;
import domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final UserServiceAPI userService;
    private final GenericKafkaConsumer<String> consumerEbike;
    private final GenericKafkaConsumer<String> consumerAbike;
    private final GenericKafkaConsumer<String> consumerAbikeEvent;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RideUpdatesConsumer(UserServiceAPI userService, String bootstrapServers) {
        this.userService = userService;
        this.consumerEbike = new GenericKafkaConsumer<>(
            bootstrapServers,
            "user-service-group"+UUID.randomUUID(),
            "ride-ebike-events",
            String.class
        );
        this.consumerAbike = new GenericKafkaConsumer<>(
                bootstrapServers,
                "user-service-group"+ UUID.randomUUID(),
                "ride-abike-events",
                String.class
        );

        this.consumerAbikeEvent = new GenericKafkaConsumer<>(
                bootstrapServers,
                "user-service-group"+ UUID.randomUUID(),
                "abike-events",
                String.class
        );
        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        consumerEbike.start(this::processUserUpdate);
        consumerAbike.start(this::processUserUpdate);
        consumerAbikeEvent.start(this::processAbikeUpdate);
        logger.info("RideUpdatesConsumer started - listening for user updates from ride service");
    }

    private void processAbikeUpdate(String key, String eventJson) {
        try {
            if (key.equals("CallAbikeEvent")) {
                var eventJsonNode = objectMapper.readTree(eventJson);
                var destination = eventJsonNode.get("destination");
                var id = destination.get("id").asText();
                userService.abikeRequested(new UserRequestedAbike(id));
            }
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    private void processUserUpdate(String key, String eventJson) {
        try {
            if ("RideUpdateABikeEvent".equals(key) || "RideUpdateEBikeEvent".equals(key)) {
                // Deserialize RideUpdateEvent
                logger.info("Updating form ride event: {}", eventJson);
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