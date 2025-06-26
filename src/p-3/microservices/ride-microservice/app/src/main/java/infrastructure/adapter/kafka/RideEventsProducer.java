package infrastructure.adapter.kafka;

import application.ports.EventPublisher;
import application.ports.RideEventsProducerPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.*;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RideEventsProducer implements RideEventsProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(RideEventsProducer.class);
    private final GenericKafkaProducer<String> rideEBikeProducer;
    private final GenericKafkaProducer<String> rideABikeProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Vertx vertx;

    public RideEventsProducer(String bootstrapServers, Vertx vertx) {
        this.rideEBikeProducer = new GenericKafkaProducer<>(bootstrapServers, "ride-ebike-events");
        this.rideABikeProducer = new GenericKafkaProducer<>(bootstrapServers, "ride-abike-events");
        logger.info("RideEventsProducer initialized with bootstrap servers: {}", bootstrapServers);
        this.vertx = vertx;
    }

    @Override
    public void init() {
        vertx.eventBus().consumer(EventPublisher.RIDE_UPDATE, message -> {
            if (message.body() instanceof RideUpdateEBikeEvent update) {
                this.publishUpdate(update);
            } else if (message.body() instanceof RideUpdateABikeEvent update) {
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
            if(event instanceof RideUpdateABikeEvent){
                rideABikeProducer.send(key, json);
            }else if(event instanceof RideUpdateEBikeEvent){
                rideEBikeProducer.send(key, json);
            }else if(event instanceof RideStartEvent start) {
                if(start.type().equals("abike")){
                    rideABikeProducer.send(key, json);
                }else {
                    rideEBikeProducer.send(key, json);
                }
            }else if(event instanceof RideStopEvent stop) {
                if(stop.type().equals("abike")){
                    rideABikeProducer.send(key, json);
                }else {
                    rideEBikeProducer.send(key, json);
                }
            }
            logger.info("Published ride event [{}]: {}", key, json);
        } catch (Exception e) {
            logger.error("Failed to publish ride event", e);
        }
    }

    public void close() {
        rideEBikeProducer.close();
        rideABikeProducer.close();
        logger.info("RideEventsProducer closed");
    }
}
