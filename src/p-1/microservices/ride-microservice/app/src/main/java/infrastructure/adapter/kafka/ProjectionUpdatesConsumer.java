package infrastructure.adapter.kafka;

import application.ports.ProjectionRepositoryPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.EBikeUpdateEvent;
import domain.event.UserUpdateEvent;
import domain.model.EBikeState;
import domain.model.P2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ProjectionUpdatesConsumer.class);

    private final GenericKafkaConsumer<String> userConsumer;
    private final GenericKafkaConsumer<String> ebikeConsumer;
    private final ProjectionRepositoryPort projectionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProjectionUpdatesConsumer(
            String bootstrapServers,
            ProjectionRepositoryPort projectionRepository) {

        this.projectionRepository = projectionRepository;

        this.userConsumer = new GenericKafkaConsumer<>(
                bootstrapServers,
                "ride-service-user-group",
                "user-events",
                String.class
        );

        this.ebikeConsumer = new GenericKafkaConsumer<>(
                bootstrapServers,
                "ride-service-ebike-group",
                "ebike-events",
                String.class
        );

        logger.info("ProjectionUpdatesConsumer created");
    }

    public void init() {
        userConsumer.start(this::processUserEvent);
        ebikeConsumer.start(this::processEbikeEvent);
        logger.info("ProjectionUpdatesConsumer started");
    }

    private void processUserEvent(String key, String message) {
        if (!"UserUpdateEvent".equals(key)) {
            logger.debug("Ignored user event with key: {}", key);
            return;
        }
        try {
            logger.info("Received user event: {}", message);

            JsonNode node = objectMapper.readTree(message);
            String id = node.get("id").asText();
            String timestamp = node.get("timestamp").asText();

            JsonNode userNode = node.get("user");
            String username = userNode.get("username").asText();
            int credit = userNode.get("credit").asInt();

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

    private void processEbikeEvent(String key, String message) {
        if (!"EBikeUpdateEvent".equals(key)) {
            logger.debug("Ignored ebike event with key: {}", key);
            return;
        }
        try {
            logger.info("Received ebike event: {}", message);

            JsonNode node = objectMapper.readTree(message);
            String id = node.get("id").asText();
            String timestamp = node.get("timestamp").asText();

            JsonNode ebikeNode = node.get("ebike");
            String bikeId = ebikeNode.get("id").asText();
            String stateStr = ebikeNode.get("state").asText();
            int batteryLevel = ebikeNode.get("batteryLevel").asInt();
            JsonNode locationNode = ebikeNode.get("location");
            double x = locationNode.get("x").asDouble();
            double y = locationNode.get("y").asDouble();

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
