package infrastructure.adapter.kafka;

import application.port.EventPublisher;
import domain.event.*;
import events.avro.*;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ABikeEventsProducer implements EventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(ABikeEventsProducer.class);
    private final KafkaProducer<String, Object> producer;

    public ABikeEventsProducer(String bootstrapServers, String schemaRegistryUrl) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.producer = new KafkaProducer<>(props);
    }

    @Override
    public void publish(Event event) {
        if (event == null) {
            logger.warn("Attempted to send null abike event");
            return;
        }
        try {
            ABikeEventsEnvelope envelope = mapToEnvelope(event);
            String key = extractKey(event);

            ProducerRecord<String, Object> record = new ProducerRecord<>("abike-events", key, envelope);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Error sending abike event", exception);
                } else {
                    logger.info("Published abike event [{}] offset={}", key, metadata.offset());
                }
            });
        } catch (Exception e) {
            logger.error("Failed to publish abike event", e);
        }
    }

    private ABikeEventsEnvelope mapToEnvelope(Event event) {
        Object avro = null;
        if (event instanceof domain.event.ABikeUpdate e) {
            avro = events.avro.ABikeUpdate.newBuilder()
                    .setId(e.id())
                    .setTimestamp(e.timestamp())
                    .setAbike(events.avro.ABike.newBuilder()
                            .setId(e.abike().id())
                            .setX(e.abike().position().x())
                            .setY(e.abike().position().y())
                            .setState(e.abike().state().toString())
                            .setBatteryLevel(e.abike().batteryLevel())
                            .build())
                    .build();
        } else if (event instanceof domain.event.CallAbikeEvent e) {
            avro = events.avro.CallAbikeEvent.newBuilder()
                    .setId(e.id())
                    .setTimestamp(e.timestamp())
                    .setDestination(events.avro.Destination.newBuilder()
                            .setLocation(events.avro.P2d.newBuilder()
                                    .setX(e.destination().position().x())
                                    .setY(e.destination().position().y())
                                    .build())
                            .setId(e.destination().id())
                            .build())
                    .build();
        } else if (event instanceof domain.event.ABikeCallComplete e) {
            avro = events.avro.ABikeCallComplete.newBuilder()
                    .setId(e.id())
                    .setBikeId(e.bikeId())
                    .setUserId(e.userId())
                    .setTimestamp(e.timestamp())
                    .build();
        } else if (event instanceof domain.event.ABikeArrivedToStation e) {
            avro = events.avro.ABikeArrivedToStation.newBuilder()
                    .setId(e.id())
                    .setBikeId(e.bikeId())
                    .setStationId(e.stationId())
                    .setTimestamp(e.timestamp())
                    .build();
        } else if (event instanceof domain.event.ABikeArrivedToUser e) {
            avro = events.avro.ABikeArrivedToUser.newBuilder()
                    .setId(e.id())
                    .setAbikeId(e.abikeId())
                    .setUserId(e.userId())
                    .setTimestamp(e.timestamp())
                    .build();
        } else if (event instanceof domain.event.ABikeRequested e) {
            avro = events.avro.ABikeRequested.newBuilder()
                    .setId(e.id())
                    .setAbikeId(e.abikeId())
                    .setUsername(e.username())
                    .setStationId(e.stationId())
                    .setTimestamp(e.timestamp())
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
        }
        return ABikeEventsEnvelope.newBuilder().setEvent(avro).build();
    }

    private String extractKey(Event event) {
        if (event instanceof domain.event.ABikeUpdate e) return e.abike().id();
        if (event instanceof domain.event.ABikeArrivedToStation e) return e.bikeId();
        if (event instanceof domain.event.ABikeArrivedToUser e) return e.abikeId();
        if (event instanceof domain.event.ABikeCallComplete e) return e.bikeId();
        if (event instanceof domain.event.CallAbikeEvent e) return e.destination().id();
        if (event instanceof domain.event.ABikeRequested e) return e.abikeId();
        return event.getId();
    }

    public void close() {
        producer.close();
    }
}
