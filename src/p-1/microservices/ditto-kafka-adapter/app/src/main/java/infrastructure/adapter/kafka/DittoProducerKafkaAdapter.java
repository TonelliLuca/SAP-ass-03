package infrastructure.adapter.kafka;

import application.port.DittoProducerPort;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class DittoProducerKafkaAdapter implements DittoProducerPort {
    private final KafkaProducer<String, String> producer;
    private final String topic;

    public DittoProducerKafkaAdapter(String bootstrapServers, String topic) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.producer = new KafkaProducer<>(props);
        this.topic = topic;
    }

    @Override
    public void send(String key, String dittoJson) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, dittoJson);
        producer.send(record);
    }

    public void close() {
        producer.close();
    }
}