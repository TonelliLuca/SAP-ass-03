package infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;

public class GenericKafkaProducer<T> {

    private final KafkaProducer<String, String> kafkaProducer;
    private final String topic;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(GenericKafkaProducer.class);
    public GenericKafkaProducer(String bootstrapServers, String topic) {
        this.topic = topic;
        Properties props = new Properties();
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1000000); // 1MB limit
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB buffer
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        this.kafkaProducer = new KafkaProducer<>(props);
    }

    public void send(String key, T value) {
        try {
            // For String values, use them directly
            String messageValue = (value instanceof String) ? (String)value : objectMapper.writeValueAsString(value);
            log.info("Message size: {} bytes", messageValue.getBytes().length);
            if (messageValue.getBytes().length > 900000) {
                log.error("Message too large, will be rejected by Kafka");
            }            log.info("Sending message {} to topic {}", messageValue, topic);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, messageValue);
            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Error sending record", exception);
                } else {
                    log.info("Record sent to topic {} partition {} offset {}",
                             metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        } catch (Exception e) {
            log.error("Error sending record", e);
        }
    }

    public void close() {
        kafkaProducer.close();
    }
}