package infrastructure.adapters.kafka;

import application.ports.EbikeProducerPort;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class EbikeUpdatesProducer implements EbikeProducerPort {
    private final GenericKafkaProducer<JsonObject> ebikeProducer;

    public EbikeUpdatesProducer(String bootstrapServers) {
        this.ebikeProducer = new GenericKafkaProducer<>(bootstrapServers, "ebike-events");
    }

    @Override
    public void sendUpdate(JsonObject ebike) {
        // Create event envelope
        JsonObject event = new JsonObject()
                .put("type", "ebike_updated")
                .put("timestamp", System.currentTimeMillis())
                .put("payload", ebike);

        ebikeProducer.send(ebike.getString("id"), event);
    }

    @Override
    public void sendAllUpdates(JsonArray ebikes) {
        // Create event envelope
        JsonObject event = new JsonObject()
                .put("type", "ebikes_batch_updated")
                .put("timestamp", System.currentTimeMillis())
                .put("payload", ebikes);

        ebikeProducer.send("batch-" + System.currentTimeMillis(), event);
    }

    public void init() {
        System.out.println("EbikeUpdatesAdapter initialized");
    }

    public void close() {
        ebikeProducer.close();
    }
}