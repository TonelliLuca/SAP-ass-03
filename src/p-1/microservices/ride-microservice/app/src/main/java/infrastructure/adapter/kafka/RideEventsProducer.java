package infrastructure.adapter.kafka;

import application.ports.EventPublisher;
import application.ports.RideEventsProducerPort;
import domain.event.Event;
import domain.event.RideStartEvent;
import domain.event.RideStopEvent;
import domain.event.RideUpdateEvent;
import events.avro.RideEventUnion;
import events.avro.RideStartEventAvro;
import events.avro.RideStopEventAvro;
import events.avro.RideUpdateEventAvro;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.vertx.core.Vertx;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;

public class RideEventsProducer implements RideEventsProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(RideEventsProducer.class);
    private final KafkaProducer<String, Object> rideProducer;
    private final Vertx vertx;

    public RideEventsProducer(String bootstrapServers, String schemaRegistryUrl, Vertx vertx) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1000000);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", true);
        this.rideProducer = new KafkaProducer<>(props);
        this.vertx = vertx;
        logger.info("RideEventsProducer initialized with bootstrap servers: {}", bootstrapServers);
    }

    @Override
    public void init() {
        vertx.eventBus().consumer(EventPublisher.RIDE_UPDATE, message -> {
            if (message.body() instanceof Event event) {
                this.publishUpdate(event);
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
            Object avroEvent = null;
            String key;

            if (event instanceof RideStartEvent e) {
                key = e.bikeId() + ":" + e.username();
                avroEvent = RideStartEventAvro.newBuilder()
                        .setId(e.id())
                        .setUsername(e.username())
                        .setBikeId(e.bikeId())
                        .setTimestamp(e.timestamp())
                        .build();
            } else if (event instanceof RideStopEvent e) {
                key = e.bikeId() + ":" + e.username();
                avroEvent = RideStopEventAvro.newBuilder()
                        .setId(e.id())
                        .setUsername(e.username())
                        .setBikeId(e.bikeId())
                        .setTimestamp(e.timestamp())
                        .build();
            } else if (event instanceof RideUpdateEvent e) {
                key = e.bikeId() + ":" + e.userId();
                avroEvent = RideUpdateEventAvro.newBuilder()
                        .setId(e.id())
                        .setRideId(e.rideId())
                        .setUserId(e.userId())
                        .setUserCredit(e.userCredit())
                        .setBikeId(e.bikeId())
                        .setBikeX(e.bikeX())
                        .setBikeY(e.bikeY())
                        .setBikeState(e.bikeState())
                        .setBikeBattery(e.bikeBattery())
                        .setTimestamp(e.timestamp())
                        .build();
            } else {
                key = "";
                throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
            }

            RideEventUnion unionEvent = RideEventUnion.newBuilder()
                    .setPayload(avroEvent)
                    .build();

            ProducerRecord<String, Object> record = new ProducerRecord<>("ride-events", key, unionEvent);
            rideProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Error sending ride event", exception);
                } else {
                    logger.info("Published ride event [{}] offset={}", key, metadata.offset());
                }
            });

        } catch (Exception e) {
            logger.error("Failed to publish ride event", e);
        }
    }

    public void close() {
        rideProducer.close();
        logger.info("RideEventsProducer closed");
    }
}