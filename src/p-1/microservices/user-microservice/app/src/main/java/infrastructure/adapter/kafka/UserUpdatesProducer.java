package infrastructure.adapter.kafka;

import application.ports.UserProducerPort;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserUpdatesProducer implements UserProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(UserUpdatesProducer.class);
    private final GenericKafkaProducer<JsonObject> userProducer;

    public UserUpdatesProducer(String bootstrapServers) {
        this.userProducer = new GenericKafkaProducer<>(bootstrapServers, "user-events");
    }

    @Override
    public void sendUpdate(JsonObject update) {
        if (update == null) {
            logger.warn("Attempted to send update with null user");
            return;
        }

        try {
            JsonObject event = new JsonObject()
                    .put("type", "user_updated")
                    .put("timestamp", System.currentTimeMillis())
                    .put("payload", update);

            userProducer.send(update.getString("username", "unknown-user"), event);
            logger.debug("Sent user update for: {}", update.getString("username", "unknown-user"));
        } catch (Exception e) {
            logger.error("Error sending user update", e);
        }
    }

    @Override
    public void sendAllUserUpdate(JsonArray updates) {
        if (updates == null || updates.isEmpty()) {
            logger.warn("Attempted to send batch update with null or empty users");
            return;
        }

        try {
            JsonObject event = new JsonObject()
                    .put("type", "users_batch_updated")
                    .put("timestamp", System.currentTimeMillis())
                    .put("payload", updates);

            userProducer.send("batch-" + System.currentTimeMillis(), event);
            logger.debug("Sent batch update with {} users", updates.size());
        } catch (Exception e) {
            logger.error("Error sending users batch update", e);
        }
    }

    public void init() {
        logger.info("UserUpdatesProducer initialized");
    }

    public void close() {
        try {
            userProducer.close();
            logger.info("UserUpdatesProducer closed");
        } catch (Exception e) {
            logger.error("Error closing UserUpdatesProducer", e);
        }
    }
}