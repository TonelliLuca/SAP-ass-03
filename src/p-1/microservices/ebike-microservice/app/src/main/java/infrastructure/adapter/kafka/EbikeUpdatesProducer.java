package infrastructure.adapter.kafka;

import application.ports.EbikeProducerPort;
import domain.event.EBikeUpdateEvent;
import domain.event.Event;
import events.avro.EBikeUpdateEventAvro;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.generic.GenericRecord;

import java.util.Properties;

public class EbikeUpdatesProducer implements EbikeProducerPort {
    private final KafkaProducer<String, Object> producer;

    public EbikeUpdatesProducer(String bootstrapServers, String schemaRegistryUrl) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1000000);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", true);

        this.producer = new KafkaProducer<>(props);

    }

    @Override
    public void sendUpdate(Event event) {
        if (event == null) return;
        try {


            if (event instanceof EBikeUpdateEvent evt) {
                // Mapping da POJO → Avro
                EBikeUpdateEventAvro avroEvent = toAvro(evt);
                String key = evt.ebike().getId();
                producer.send(new ProducerRecord<>("ebike-events", key, avroEvent));
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
            }
        } catch (Exception e) {
            // log error
        }
    }


    public void close() {
        producer.close();
    }

    public static events.avro.EBikeUpdateEventAvro toAvro(domain.event.EBikeUpdateEvent evt) {
        domain.model.EBike ebikeDomain = evt.ebike();
        events.avro.EBike avroEbike = events.avro.EBike.newBuilder()
                .setId(ebikeDomain.getId())
                .setLocationX(ebikeDomain.getLocation().getX())
                .setLocationY(ebikeDomain.getLocation().getY())
                .setState(ebikeDomain.getState().name())
                .setBatteryLevel(ebikeDomain.getBatteryLevel())
                .build();

        return events.avro.EBikeUpdateEventAvro.newBuilder()
                .setId(evt.id())
                .setEbike(avroEbike)
                .setTimestamp(evt.timestamp())
                .build();
    }

}
