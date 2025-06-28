package infrastructure.adapter.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import application.ports.EBikeServiceAPI;
import domain.event.RequestEBikeUpdateEvent;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final EBikeServiceAPI ebikeService;
    private final KafkaConsumer<String, GenericRecord> consumer;

    public RideUpdatesConsumer(EBikeServiceAPI ebikeService, String bootstrapServers, String schemaRegistryUrl) {
        this.ebikeService = ebikeService;

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ebike-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", false); // GENERIC RECORD
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("ride-events"));
        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void start() {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    ConsumerRecords<String, GenericRecord> records = consumer.poll(Duration.ofMillis(500));
                    for (ConsumerRecord<String, GenericRecord> record : records) {
                        try {
                            GenericRecord unionRecord = record.value();
                            // RideEventUnion: estrai il vero evento!
                            GenericRecord payload = (GenericRecord) unionRecord.get("payload");
                            if (payload == null) continue;

                            // Filtra SOLO RideUpdateEventAvro!
                            if (!"RideUpdateEventAvro".equals(payload.getSchema().getName())) continue;

                            String bikeId = payload.get("bikeId").toString();
                            double bikeX = (double) payload.get("bikeX");
                            double bikeY = (double) payload.get("bikeY");
                            String bikeState = payload.get("bikeState").toString();
                            int bikeBattery = (int) payload.get("bikeBattery");

                            RequestEBikeUpdateEvent ebikeUpdateEvent = new RequestEBikeUpdateEvent(
                                    bikeId, bikeX, bikeY, bikeState, bikeBattery
                            );

                            ebikeService.updateEBike(ebikeUpdateEvent)
                                    .whenComplete((result, throwable) -> {
                                        if (throwable != null) {
                                            logger.error("Failed to update e-bike: {}", throwable.getMessage());
                                        } else if (result == null) {
                                            logger.warn("E-bike with id {} not found", bikeId);
                                        } else {
                                            logger.info("Successfully updated e-bike: {}", bikeId);
                                        }
                                    });
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

    public void stop() {
        consumer.wakeup();
        logger.info("Kafka consumer stopping");
    }
}
