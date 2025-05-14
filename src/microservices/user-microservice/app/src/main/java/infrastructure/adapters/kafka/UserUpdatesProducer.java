package infrastructure.adapters.kafka;

import application.ports.UserProducerPort;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class UserUpdatesProducer implements UserProducerPort {
    private final GenericKafkaProducer<JsonObject> userProducer;

    public UserUpdatesProducer(String bootstrapServers) {
        this.userProducer = new GenericKafkaProducer<>(bootstrapServers, "user-events");
    }

    @Override
    public void sendUpdate(JsonObject update) {
        // Create event envelope
        JsonObject event = new JsonObject()
                .put("type", "user_updated")
                .put("timestamp", System.currentTimeMillis())
                .put("payload", update);

        userProducer.send(update.getString("username"), event);
    }

    @Override
    public void sendAllUserUpdate(JsonArray updates) {
        // Create event envelope for batch updates
        JsonObject event = new JsonObject()
                .put("type", "users_batch_updated")
                .put("timestamp", System.currentTimeMillis())
                .put("payload", updates);

        userProducer.send("batch-" + System.currentTimeMillis(), event);
    }

    public void init() {
        System.out.println("UserUpdatesProducer initialized");
    }

    public void close() {
        userProducer.close();
    }
}