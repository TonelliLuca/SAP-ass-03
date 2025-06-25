package infrastructure.adapter.kafka;

import application.ports.EBikeServiceAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.RequestEBikeUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final EBikeServiceAPI ebikeService;
    private final GenericKafkaConsumer<String> consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RideUpdatesConsumer(EBikeServiceAPI ebikeService, String bootstrapServers) {
        this.ebikeService = ebikeService;
        this.consumer = new GenericKafkaConsumer<>(
            bootstrapServers,
            "ebike-service-group",
            "ride-events",
            String.class
        );
        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        consumer.start(this::processRideEvent);
        logger.info("RideUpdatesConsumer started - listening for e-bike updates from ride service");
    }

    private void processRideEvent(String key, String eventJson) {
        try {
            if (!"RideUpdateEvent".equals(key)) {
                return;
            }
            JsonNode node = objectMapper.readTree(eventJson);

            String bikeId = node.get("bikeId").asText();
            double bikeX = node.get("bikeX").asDouble();
            double bikeY = node.get("bikeY").asDouble();
            String bikeState = node.get("bikeState").asText();
            int bikeBattery = node.get("bikeBattery").asInt();

            RequestEBikeUpdateEvent ebikeUpdateEvent = new RequestEBikeUpdateEvent(
                bikeId, bikeX, bikeY, bikeState, bikeBattery
            );

            ebikeService.updateEBike(ebikeUpdateEvent)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Failed to update e-bike: {}", throwable.getMessage());
                    } else if (result == null) {
                        logger.warn("E-bike with id {} not found", bikeId);
                    } else {
                        logger.info("Successfully updated e-bike: {}", bikeId);
                    }
                });
        } catch (Exception e) {
            logger.error("Error processing RideUpdateEvent", e);
        }
    }
}