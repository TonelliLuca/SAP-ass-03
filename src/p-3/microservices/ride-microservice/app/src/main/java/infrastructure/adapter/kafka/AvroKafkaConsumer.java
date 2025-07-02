package infrastructure.adapter.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.function.BiConsumer;

public class AvroKafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(AvroKafkaConsumer.class);
    private final KafkaConsumer<String, GenericRecord> kafkaConsumer;
    private volatile boolean running = true;

    public AvroKafkaConsumer(String bootstrapServers, String schemaRegistryUrl, String groupId, String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", false); // Usa generic
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        this.kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Collections.singletonList(topic));
        logger.info("Subscribed to topic: {}", topic);
    }

    public void start(BiConsumer<String, GenericRecord> handler) {
        Thread thread = new Thread(() -> {
            try {
                while (running) {
                    ConsumerRecords<String, GenericRecord> records = kafkaConsumer.poll(Duration.ofMillis(500));
                    for (ConsumerRecord<String, GenericRecord> record : records) {
                        try {
                            handler.accept(record.key(), record.value());
                        } catch (Exception e) {
                            logger.error("Error processing Avro record: {}", e.getMessage(), e);
                        }
                    }
                }
            } catch (org.apache.kafka.common.errors.WakeupException e) {
                logger.info("Kafka consumer woke up for shutdown");
            } catch (Exception e) {
                logger.error("Kafka consumer error", e);
            } finally {
                synchronized (kafkaConsumer) {
                    kafkaConsumer.close();
                }
                logger.info("Kafka consumer closed");
            }
        });
        thread.setDaemon(false);
        thread.start();
        logger.info("Kafka Avro consumer thread started");
    }

    public void stop() {
        running = false;
        synchronized (kafkaConsumer) {
            kafkaConsumer.wakeup();
        }
        logger.info("Kafka consumer stopping");
    }
}
