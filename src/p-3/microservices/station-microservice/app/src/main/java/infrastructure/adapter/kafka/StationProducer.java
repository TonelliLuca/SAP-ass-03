package infrastructure.adapter.kafka;

import application.ports.DomainEventPublisher;
import domain.event.Event;
import domain.event.StationRegisteredEvent;
import domain.event.StationUpdateEvent;
import events.avro.StationRegisteredEventAvro;
import events.avro.StationUpdateEventAvro;
import events.avro.StationEventsEnvelope;
import events.avro.P2d;
import events.avro.Station;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

public class StationProducer implements DomainEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(StationProducer.class);
    private final KafkaProducer<String, Object> kafkaProducer;

    public StationProducer(String bootstrapServers, String schemaRegistryUrl) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.kafkaProducer = new KafkaProducer<>(props);
    }

    @Override
    public void publish(Event event) {
        if (event == null) return;
        try {
            StationEventsEnvelope envelope = buildEnvelope(event);
            String key = extractKey(event);
            ProducerRecord<String, Object> record = new ProducerRecord<>("station-events", key, envelope);

            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    LOG.error("Error sending event to Kafka", exception);
                } else {
                    LOG.info("Published station event [{}] offset={}", key, metadata.offset());
                }
            });
        } catch (Exception e) {
            LOG.error("Failed to publish station event", e);
        }
    }

    private StationEventsEnvelope buildEnvelope(Event event) {
        if (event instanceof StationRegisteredEvent e) {
            StationRegisteredEventAvro avro = StationRegisteredEventAvro.newBuilder()
                    .setId(e.id())
                    .setTimestamp(e.timestamp())
                    .setStation(toAvroStation(e.station()))
                    .build();
            return StationEventsEnvelope.newBuilder()
                    .setEvent(avro)
                    .build();
        } else if (event instanceof StationUpdateEvent e) {
            StationUpdateEventAvro avro = StationUpdateEventAvro.newBuilder()
                    .setId(e.id())
                    .setTimestamp(e.timestamp())
                    .setStation(toAvroStation(e.station()))
                    .build();
            return StationEventsEnvelope.newBuilder()
                    .setEvent(avro)
                    .build();
        }
        throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
    }

    private String extractKey(Event event) {
        // Usa station id come key (o quello che preferisci)
        if (event instanceof StationRegisteredEvent e) return e.station().getId();
        if (event instanceof StationUpdateEvent e) return e.station().getId();
        return event.getId();
    }

    private Station toAvroStation(domain.model.Station station) {
        // Mappa Station Java -> Avro
        P2d avroP2d = P2d.newBuilder()
                .setX(station.getLocation().x())
                .setY(station.getLocation().y())
                .build();

        // Set dockedBikes come lista
        ArrayList<String> dockedBikes = new ArrayList<>(station.getDockedBikes());

        return Station.newBuilder()
                .setId(station.getId())
                .setLocation(avroP2d)
                .setCapacity(station.getCapacity())
                .setDockedBikes(dockedBikes)
                .build();
    }

    @Override
    public void close() {
        kafkaProducer.close();
        LOG.info("Producer closed");
    }
}
