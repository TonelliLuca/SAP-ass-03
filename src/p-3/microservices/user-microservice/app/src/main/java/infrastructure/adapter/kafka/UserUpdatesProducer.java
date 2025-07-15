package infrastructure.adapter.kafka;

import application.ports.UserProducerPort;
import domain.event.Event;
import domain.event.UserUpdateEvent;
import domain.model.User;
import events.avro.UserUpdateEventAvro;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class UserUpdatesProducer implements UserProducerPort {
    private final KafkaProducer<String, Object> userProducer;

    public UserUpdatesProducer(String bootstrapServers, String schemaRegistryUrl) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1000000);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", true);

        this.userProducer = new KafkaProducer<>(props);
    }

    @Override
    public void sendUpdate(Event event) {
        if (event == null) return;
        try {
            if (event instanceof UserUpdateEvent userUpdateEvent) {
                User user = userUpdateEvent.user();
                events.avro.User avroUser = events.avro.User.newBuilder()
                        .setUsername(user.getId())
                        .setCredit(user.getCredit())
                        .build();

                UserUpdateEventAvro avroEvent = UserUpdateEventAvro.newBuilder()
                        .setId(userUpdateEvent.id())
                        .setUser(avroUser)
                        .setTimestamp(userUpdateEvent.timestamp())
                        .build();

                String key = user.getId();
                ProducerRecord<String, Object> record = new ProducerRecord<>("user-events", key, avroEvent);

                userProducer.send(record, (metadata, exception) -> {
                    if (exception != null) exception.printStackTrace();
                });
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        userProducer.close();
    }
}
