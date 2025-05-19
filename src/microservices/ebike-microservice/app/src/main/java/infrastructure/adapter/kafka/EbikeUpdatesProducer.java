package infrastructure.adapter.kafka;

import application.ports.EbikeProducerPort;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EbikeUpdatesProducer implements EbikeProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(EbikeUpdatesProducer.class);
    private final GenericKafkaProducer<JsonObject> ebikeProducer;

    public EbikeUpdatesProducer(String bootstrapServers) {
        this.ebikeProducer = new GenericKafkaProducer<>(bootstrapServers, "ebike-events");
    }

    @Override
    public void sendUpdate(JsonObject ebike) {
        if (ebike == null) {
            logger.warn("Attempted to send update with null ebike");
            return;
        }

        try {
            JsonObject event = new JsonObject()
                    .put("type", "ebike_updated")
                    .put("timestamp", System.currentTimeMillis())
                    .put("payload", ebike);

            ebikeProducer.send(ebike.getString("id", "unknown-id"), event);
            logger.debug("Sent ebike update for ID: {}", ebike.getString("id", "unknown-id"));
        } catch (Exception e) {
            logger.error("Error sending ebike update", e);
        }
    }

    @Override
    public void sendAllUpdates(JsonArray ebikes) {
        if (ebikes == null || ebikes.isEmpty()) {
            logger.warn("Attempted to send batch update with null or empty ebikes");
            return;
        }

        try {
            JsonObject event = new JsonObject()
                    .put("type", "ebikes_batch_updated")
                    .put("timestamp", System.currentTimeMillis())
                    .put("payload", ebikes);
            logger.info("Sent ebike batch update for ebikes: {}", event);
            ebikeProducer.send("batch-" + System.currentTimeMillis(), event);
            logger.debug("Sent batch update with {} ebikes", ebikes.size());
        } catch (Exception e) {
            logger.error("Error sending ebikes batch update", e);
        }
    }


}