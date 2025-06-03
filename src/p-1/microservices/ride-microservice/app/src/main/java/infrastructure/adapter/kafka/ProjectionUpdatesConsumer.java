package infrastructure.adapter.kafka;

import application.ports.ProjectionRepositoryPort;
import application.ports.RestRideServiceAPI;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectionUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ProjectionUpdatesConsumer.class);

    private final GenericKafkaConsumer<JsonObject> userConsumer;
    private final GenericKafkaConsumer<JsonObject> ebikeConsumer;
    private final RestRideServiceAPI rideService;

    public ProjectionUpdatesConsumer(
            String bootstrapServers,
            RestRideServiceAPI rideService) {

        this.rideService = rideService;

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
            logger.info("Received user event: {}", event.encodePrettily());
            String type = event.getString("type");
            JsonObject payload = event.getJsonObject("payload");

            if ("user_updated".equals(type) && payload != null) {
                JsonObject userData = payload.containsKey("map") ? payload.getJsonObject("map") : payload;
                rideService.handleUserProjectionUpdate(userData)
                    .exceptionally(ex -> {
                        logger.error("Error updating user projection: {}", ex.getMessage());
                        return null;
                    });
                logger.info("Processing user projection update: {}", userData.getString("username"));
            } else if ("users_batch_updated".equals(type) && payload != null) {
                var userList = payload.getJsonArray("list");
                if (userList != null) {
                    userList.forEach(user -> {
                        if (user instanceof JsonObject userObj) {
                            JsonObject userData = userObj.containsKey("map") ? userObj.getJsonObject("map") : userObj;
                            rideService.handleUserProjectionUpdate(userData)
                                .exceptionally(ex -> {
                                    logger.error("Error updating user in batch: {}", ex.getMessage());
                                    return null;
                                });
                        }
                    });
                    logger.info("Processing batch user update");
                }
            }
        } catch (Exception e) {
            logger.error("Error processing user event", e);
        }
    }

    private void processEbikeEvent(String key, JsonObject event) {
        try {
            logger.info("Received ebike event: {}", event.encodePrettily());
            String type = event.getString("type");
            JsonObject payload = event.getJsonObject("payload");

            if ("ebike_updated".equals(type) && payload != null) {
                JsonObject ebikeData = payload.containsKey("map") ? payload.getJsonObject("map") : payload;
                rideService.handleEBikeProjectionUpdate(ebikeData)
                    .exceptionally(ex -> {
                        logger.error("Error updating ebike projection: {}", ex.getMessage());
                        return null;
                    });
                logger.info("Processing ebike projection update: {}", ebikeData.getString("id"));
            } else if ("ebikes_batch_updated".equals(type) && payload != null) {
                var ebikeList = payload.getJsonArray("list");
                if (ebikeList != null) {
                    ebikeList.forEach(ebike -> {
                        if (ebike instanceof JsonObject ebikeObj) {
                            JsonObject ebikeData = ebikeObj.containsKey("map") ? ebikeObj.getJsonObject("map") : ebikeObj;
                            rideService.handleEBikeProjectionUpdate(ebikeData)
                                .exceptionally(ex -> {
                                    logger.error("Error updating ebike in batch: {}", ex.getMessage());
                                    return null;
                                });
                        }
                    });
                    logger.info("Processing batch ebike update");
                }
            }
        } catch (Exception e) {
            logger.error("Error processing ebike event", e);
        }
    }

}