package infrastructure.adapter.kafka;

import application.port.DittoProducerPort;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
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
        ProducerRecord<String, String> record =
                (key == null || key.trim().isEmpty())
                        ? new ProducerRecord<>(topic, null, dittoJson)
                        : new ProducerRecord<>(topic, key.trim(), dittoJson);
        producer.send(record);
    }

    @Override
    public void sendWithCorrelationHeader(String key, String dittoJson) {
        ProducerRecord<String, String> record =
                (key == null || key.trim().isEmpty())
                        ? new ProducerRecord<>(topic, null, dittoJson)
                        : new ProducerRecord<>(topic, key.trim(), dittoJson);

        // Extract correlation-id from JSON and add as Kafka header
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(dittoJson);
            String correlationId = root.path("headers").path("correlation-id").asText(null);
            if (correlationId != null) {
                record.headers().add(
                        "correlation-id",
                        correlationId.getBytes(StandardCharsets.UTF_8)
                );
            }
        } catch (Exception e) {
            // log or ignore
        }

        producer.send(record);
    }

    public void close() {
        producer.close();
    }
}