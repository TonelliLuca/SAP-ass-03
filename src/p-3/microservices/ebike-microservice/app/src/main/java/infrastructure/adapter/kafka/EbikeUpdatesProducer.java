package infrastructure.adapter.kafka;

import application.ports.EbikeProducerPort;
import domain.event.EBikeUpdateEvent;
import events.avro.EBikeUpdateEventAvro;
import events.avro.EBike;
import events.avro.P2d;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EbikeUpdatesProducer implements EbikeProducerPort {
    private static final Logger logger = LoggerFactory.getLogger(EbikeUpdatesProducer.class);
    private final KafkaProducer<String, Object> ebikeProducer;

    public EbikeUpdatesProducer(String bootstrapServers, String schemaRegistryUrl) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.ebikeProducer = new KafkaProducer<>(props);
    }

    @Override
    public void sendUpdate(domain.event.Event event) {
        if (!(event instanceof EBikeUpdateEvent e)) {
            logger.warn("Attempted to send invalid event type: {}", event == null ? "null" : event.getClass());
            return;
        }
        try {
            EBikeUpdateEventAvro avroEvent = toAvro(e);
            String key = e.ebike().getId();
            ProducerRecord<String, Object> record = new ProducerRecord<>("ebike-events", key, avroEvent);
            ebikeProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to publish ebike event", exception);
                } else {
                    logger.info("Published ebike event [{}] offset={}", key, metadata.offset());
                }
            });
        } catch (Exception ex) {
            logger.error("Failed to produce ebike update", ex);
        }
    }

    private EBikeUpdateEventAvro toAvro(EBikeUpdateEvent e) {
        domain.model.EBike ebike = e.ebike();
        domain.model.P2d loc = ebike.getLocation();
        P2d avroLoc = P2d.newBuilder()
                .setX(loc.getX())
                .setY(loc.getY())
                .build();
        EBike avroEbike = EBike.newBuilder()
                .setId(ebike.getId())
                .setLocation(avroLoc)
                .setState(ebike.getState().name())
                .setBatteryLevel(ebike.getBatteryLevel())
                .build();
        return EBikeUpdateEventAvro.newBuilder()
                .setId(e.id())
                .setEbike(avroEbike)
                .setTimestamp(e.timestamp())
                .build();
    }

    public void close() {
        ebikeProducer.close();
        logger.info("EbikeUpdatesProducer closed");
    }
}
