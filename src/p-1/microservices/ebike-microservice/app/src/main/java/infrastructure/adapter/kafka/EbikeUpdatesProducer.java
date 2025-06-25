package infrastructure.adapter.kafka;

import application.ports.EbikeProducerPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EbikeUpdatesProducer implements EbikeProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(EbikeUpdatesProducer.class);
    private final GenericKafkaProducer<String> ebikeProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EbikeUpdatesProducer(String bootstrapServers) {
        this.ebikeProducer = new GenericKafkaProducer<>(bootstrapServers, "ebike-events");
    }

    @Override
    public void sendUpdate(Event event) {
        if (event == null) {
            logger.warn("Attempted to send update with null event");
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            String key = event.getClass().getSimpleName();
            ebikeProducer.send(key, json);
            logger.info("Published ebike event: {}", json);
        } catch (Exception e) {
            logger.error("Failed to publish ebike event", e);
        }
    }

    public void close() {
        ebikeProducer.close();
        logger.info("EbikeUpdatesProducer closed");
    }
}