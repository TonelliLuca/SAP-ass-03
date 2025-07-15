package infrastructure.adapter.kafka;

import application.ports.UserServiceAPI;
import domain.event.RequestUserUpdateEvent;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final UserServiceAPI userService;
    private final KafkaConsumer<String, GenericRecord> consumer;

    public RideUpdatesConsumer(UserServiceAPI userService, String bootstrapServers, String schemaRegistryUrl) {
        this.userService = userService;

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "user-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", false); // Usa generic record
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("ride-events"));
        logger.info("RideUpdatesConsumer (user) created with bootstrap servers: {}", bootstrapServers);
    }

    public void start() {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(500));
                    for (ConsumerRecord<String, GenericRecord> record : records) {
                        try {
                            GenericRecord unionRecord = record.value();
                            GenericRecord payload = (GenericRecord) unionRecord.get("payload");
                            if (payload == null) continue;

                            if (!"RideUpdateEventAvro".equals(payload.getSchema().getName())) continue;

                            String userId = payload.get("userId").toString();
                            int userCredit = (int) payload.get("userCredit");

                            userService.updateUser(new RequestUserUpdateEvent(userId, userCredit));
                            logger.info("Processed RideUpdateEventAvro and triggered UserUpdateEvent for userId={}", userId);
                        } catch (Exception e) {
                            logger.error("Error processing RideUpdateEventAvro", e);
                        }
                    }
                }
            } finally {
                consumer.close();
                logger.info("Kafka consumer closed");
            }
        });
        thread.setDaemon(false);
        thread.start();
        logger.info("Kafka consumer thread started");
    }
}
