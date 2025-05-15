package infrastructure.adapter.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.function.BiConsumer;

public class GenericKafkaConsumer<T> {

    private final KafkaConsumer<String, String> kafkaConsumer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Class<T> type;
    private volatile boolean running = true;

    public GenericKafkaConsumer(String bootstrapServers, String groupId, String topic, Class<T> type) {
        this.type = type;
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        this.kafkaConsumer = new KafkaConsumer<>(props);
        kafkaConsumer.subscribe(Collections.singletonList(topic));
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
                            System.err.println("Error processing record: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            } catch (WakeupException e) {
                if (running) {
                    System.err.println("Unexpected WakeupException: " + e.getMessage());
                }
            } finally {
                kafkaConsumer.close();
            }
        });
        thread.setDaemon(false);
        thread.start();
    }

    public void stop() {
        running = false;
        kafkaConsumer.wakeup();
    }
}