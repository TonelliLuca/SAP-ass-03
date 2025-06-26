package infrastructure.adapter.kafka;

import application.ports.UserProducerPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserUpdatesProducer implements UserProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(UserUpdatesProducer.class);
    private final GenericKafkaProducer<String> userProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserUpdatesProducer(String bootstrapServers) {
        this.userProducer = new GenericKafkaProducer<>(bootstrapServers, "user-events");
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
            userProducer.send(key, json);
            logger.info("Published user event: {}", json);
        } catch (Exception e) {
            logger.error("Failed to publish user event", e);
        }
    }


    public void close() {
        userProducer.close();
        logger.info("UserUpdatesProducer closed");
    }
}