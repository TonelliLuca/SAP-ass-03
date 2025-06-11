package infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.function.BiConsumer;

public class GenericKafkaConsumer<T> {
    private static final Logger logger = LoggerFactory.getLogger(GenericKafkaConsumer.class);
    private final KafkaConsumer<String, String> kafkaConsumer;
    private final ObjectMapper mapper;
    private final Class<T> type;
    private volatile boolean running = true;

    public GenericKafkaConsumer(String bootstrapServers, String groupId, String topic, Class<T> type) {
        this.type = type;

        // Configure Jackson to ignore unknown properties
        this.mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        this.kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Collections.singletonList(topic));
        logger.info("Subscribed to topic: {}", topic);
    }

    public void start(BiConsumer<String, T> handler) {
        Thread thread = new Thread(() -> {
            try {
                while (running) {
                    ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(500));
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            T value = mapper.readValue(record.value(), type);
                            handler.accept(record.key(), value);
                        } catch (Exception e) {
                            logger.error("Error processing record: {}", e.getMessage(), e);
                        }
                    }
                }
            } catch (WakeupException e) {
                if (running) {
                    logger.error("Unexpected WakeupException", e);
                }
            } finally {
                kafkaConsumer.close();
                logger.info("Kafka consumer closed");
            }
        });
        thread.setDaemon(false);
        thread.start();
        logger.info("Kafka consumer thread started");
    }

    public void stop() {
        running = false;
        kafkaConsumer.wakeup();
        logger.info("Kafka consumer stopping");
    }
}