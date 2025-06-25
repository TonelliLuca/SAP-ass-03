package infrastructure.adapter.kafka;

import application.ports.EventPublisher;
import application.ports.RideEventsProducerPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.Event;
import domain.event.RideUpdateEvent;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideEventsProducer implements RideEventsProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(RideEventsProducer.class);
    private final GenericKafkaProducer<String> rideProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Vertx vertx;

    public RideEventsProducer(String bootstrapServers, Vertx vertx) {
        this.rideProducer = new GenericKafkaProducer<>(bootstrapServers, "ride-events");
        this.vertx = vertx;
        logger.info("RideEventsProducer initialized with bootstrap servers: {}", bootstrapServers);
    }

    @Override
    public void init() {
        vertx.eventBus().consumer(EventPublisher.RIDE_UPDATE, message -> {
            if (message.body() instanceof RideUpdateEvent update) {
                this.publishUpdate(update);
            }
        });
        logger.info("RideEventsProducer init() called");
    }

    @Override
    public void publishUpdate(Event event) {
        if (event == null) {
            logger.warn("Attempted to publish null event");
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            String key = event.getClass().getSimpleName();
            rideProducer.send(key, json);
            logger.info("Published ride event [{}]: {}", key, json);
        } catch (Exception e) {
            logger.error("Failed to publish ride event", e);
        }
    }

    public void close() {
        rideProducer.close();
        logger.info("RideEventsProducer closed");
    }
}
