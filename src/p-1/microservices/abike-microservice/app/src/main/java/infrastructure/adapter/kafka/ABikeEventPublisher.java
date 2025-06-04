package infrastructure.adapter.kafka;

import application.port.EventPublisher;
import domain.events.Event;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ABikeEventPublisher implements EventPublisher {
    private final GenericKafkaProducer<JsonObject> producer;
    private final Logger logger = LoggerFactory.getLogger(ABikeEventPublisher.class);
    public ABikeEventPublisher(String bootstrapServers) {
        this.producer = new GenericKafkaProducer<>(bootstrapServers, "abike-events");
    }

    @Override
    public void publish(Event event) {
        JsonObject json = JsonObject.mapFrom(event)
            .put("type", event.getClass().getSimpleName())
            .put("timestamp", System.currentTimeMillis());
        logger.info("Publishing event: {}", json);
        producer.send(event.getClass().getSimpleName(), json);
    }

    public void close() {
        producer.close();
    }
}