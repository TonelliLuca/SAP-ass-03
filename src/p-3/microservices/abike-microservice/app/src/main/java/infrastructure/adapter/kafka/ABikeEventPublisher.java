package infrastructure.adapter.kafka;

import application.port.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.Event;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ABikeEventPublisher implements EventPublisher {
    private final GenericKafkaProducer<String> producer;
    private final Logger logger = LoggerFactory.getLogger(ABikeEventPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    public ABikeEventPublisher(String bootstrapServers) {
        this.producer = new GenericKafkaProducer<>(bootstrapServers, "abike-events");
    }

    @Override
    public void publish(Event event) {
        if (event == null) {
            logger.warn("Attempted to send update with null event");
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            String key = event.getClass().getSimpleName();
            producer.send(key, json);
            logger.info("Published ebike event: {}", json);
        } catch (Exception e) {
            logger.error("Failed to publish ebike event", e);
        }
    }

    public void close() {
        producer.close();
    }
}