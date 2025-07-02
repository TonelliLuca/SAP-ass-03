package infrastructure.adapter.kafka;

import application.ports.EventPublisher;
import application.ports.RideEventsProducerPort;
import domain.event.*;
import events.avro.*;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.vertx.core.Vertx;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class RideEventsProducer implements RideEventsProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(RideEventsProducer.class);
    private final KafkaProducer<String, Object> producer;
    private final Vertx vertx;

    public RideEventsProducer(String bootstrapServers, String schemaRegistryUrl, Vertx vertx) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.producer = new KafkaProducer<>(props);
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
            RideEventsEnvelope envelope = null;
            String key;

            // --- Mapping by instance and type ---
            if (event instanceof RideUpdateABikeEvent e) {
                envelope = RideEventsEnvelope.newBuilder()
                        .setEvent(toAvro(e))
                        .build();
                key = e.bikeId()+":"+e.userId();
            } else if (event instanceof RideUpdateEBikeEvent e) {
                envelope = RideEventsEnvelope.newBuilder()
                        .setEvent(toAvro(e))
                        .build();
                key = e.bikeId()+":"+e.userId();
            } else if (event instanceof RideStartEvent e) {
                // Se vuoi togliere type dal dominio, crea RideStartABikeEvent e RideStartEBikeEvent domain separati
                if (e.type().equalsIgnoreCase("abike")) {
                    envelope = RideEventsEnvelope.newBuilder()
                            .setEvent(toAvroStartABike(e))
                            .build();
                } else {
                    envelope = RideEventsEnvelope.newBuilder()
                            .setEvent(toAvroStartEBike(e))
                            .build();
                }
                key = e.bikeId()+":"+e.username();
            } else if (event instanceof RideStopEvent e) {
                if (e.type().equalsIgnoreCase("abike")) {
                    envelope = RideEventsEnvelope.newBuilder()
                            .setEvent(toAvroStopABike(e))
                            .build();
                } else {
                    envelope = RideEventsEnvelope.newBuilder()
                            .setEvent(toAvroStopEBike(e))
                            .build();
                }
                key = e.bikeId()+":"+e.username();
            } else {
                key = null;
            }

            if (envelope != null) {
                ProducerRecord<String, Object> record = new ProducerRecord<>("ride-events", key, envelope);
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        logger.error("Error sending ride event to Kafka", exception);
                    } else {
                        logger.info("Published ride event [{}] to ride-events offset={}", key, metadata.offset());
                    }
                });
            }

        } catch (Exception e) {
            logger.error("Failed to publish ride event", e);
        }
    }

    // ------ MAPPING METHODS --------

    private RideStartABikeEventAvro toAvroStartABike(RideStartEvent e) {
        return RideStartABikeEventAvro.newBuilder()
                .setId(e.id())
                .setUsername(e.username())
                .setBikeId(e.bikeId())
                .setTimestamp(e.timestamp())
                .build();
    }

    private RideStartEBikeEventAvro toAvroStartEBike(RideStartEvent e) {
        return RideStartEBikeEventAvro.newBuilder()
                .setId(e.id())
                .setUsername(e.username())
                .setBikeId(e.bikeId())
                .setTimestamp(e.timestamp())
                .build();
    }

    private RideStopABikeEventAvro toAvroStopABike(RideStopEvent e) {
        return RideStopABikeEventAvro.newBuilder()
                .setId(e.id())
                .setUsername(e.username())
                .setBikeId(e.bikeId())
                .setTimestamp(e.timestamp())
                .build();
    }

    private RideStopEBikeEventAvro toAvroStopEBike(RideStopEvent e) {
        return RideStopEBikeEventAvro.newBuilder()
                .setId(e.id())
                .setUsername(e.username())
                .setBikeId(e.bikeId())
                .setTimestamp(e.timestamp())
                .build();
    }

    private RideUpdateABikeEventAvro toAvro(RideUpdateABikeEvent e) {
        return RideUpdateABikeEventAvro.newBuilder()
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
    }

    private RideUpdateEBikeEventAvro toAvro(RideUpdateEBikeEvent e) {
        return RideUpdateEBikeEventAvro.newBuilder()
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
    }

    public void close() {
        producer.close();
        logger.info("RideEventsProducer closed");
    }
}
