package infrastructure.adapter.kafka;

import application.ports.DomainEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.events.StationRegisteredEvent;
import domain.model.P2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StationProducer implements DomainEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(StationProducer.class);
    private final GenericKafkaProducer<String> producer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StationProducer(String bootstrapServers) {
        this.producer = new GenericKafkaProducer<>(bootstrapServers, "station-events");
    }

    @Override
    public void publish(Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            String key = event.getClass().getSimpleName();
            producer.send(key, json);
            LOG.info("Published event: {}", json);
        } catch (Exception e) {
            LOG.error("Failed to publish event", e);
        }
    }

    @Override
    public void close() {
        producer.close();
        LOG.info("Producer closed");
    }
}