package infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class GenericKafkaProducer<T> {

    private final KafkaProducer<String, String> kafkaProducer;
    private final String topic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GenericKafkaProducer(String bootstrapServers, String topic) {
        this.topic = topic;
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.kafkaProducer = new KafkaProducer<>(props);
    }

    public void send(String key, T value) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, jsonValue);
            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Failed to send message: " + exception.getMessage());
                } else {
                    System.out.println("Message sent successfully to topic " + metadata.topic() +
                            " partition " + metadata.partition() + " offset " + metadata.offset());
                }
            });
        } catch (Exception e) {
            System.err.println("Error serializing message: " + e.getMessage());
        }
    }

    public void close() {
        kafkaProducer.close();
    }
}