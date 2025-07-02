package infrastructure.adapter.kafka;

import application.ports.UserServiceAPI;
import domain.event.RequestUserUpdateEvent;
import domain.event.UserRequestedAbike;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

public class RideUpdatesConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RideUpdatesConsumer.class);
    private final UserServiceAPI userService;
    private final AvroKafkaConsumer rideConsumer;
    private final AvroKafkaConsumer abikeConsumer;

    public RideUpdatesConsumer(UserServiceAPI userService, String bootstrapServers, String schemaRegistryUrl) {
        this.userService = userService;
        this.rideConsumer = new AvroKafkaConsumer(bootstrapServers, schemaRegistryUrl, "user-service-group-" + UUID.randomUUID(), "ride-events");
        this.abikeConsumer = new AvroKafkaConsumer(bootstrapServers, schemaRegistryUrl, "user-service-group-" + UUID.randomUUID(), "abike-events");
        logger.info("RideUpdatesConsumer created with bootstrap servers: {}", bootstrapServers);
    }

    public void init() {
        rideConsumer.start(this::processUserUpdate);
        abikeConsumer.start(this::processAbikeUpdate);
        logger.info("RideUpdatesConsumer started - listening for user updates from ride service");
    }

    private void processAbikeUpdate(String key, GenericRecord envelope) {
        try {
            // Estrai il vero evento dall'envelope
            GenericRecord event = (GenericRecord) envelope.get("event");
            if (event == null) {
                logger.error("Envelope missing event field");
                return;
            }
            String schemaName = event.getSchema().getName();
            if ("CallAbikeEvent".equals(schemaName)) {
                GenericRecord destination = (GenericRecord) event.get("destination");
                if (destination == null) {
                    logger.error("CallAbikeEvent missing destination field");
                    return;
                }
                String id = destination.get("id").toString();
                userService.abikeRequested(new UserRequestedAbike(id));
                logger.info("Processed CallAbikeEvent for userId={}", id);
            }
            // Se serve: gestisci altri eventi abike qui...
        } catch (Exception e) {
            logger.error("Error processing abike update", e);
        }
    }

    private void processUserUpdate(String key, GenericRecord envelope) {
        try {
            // Estrai il vero evento dall'envelope
            GenericRecord event = (GenericRecord) envelope.get("event");
            if (event == null) {
                logger.error("Envelope missing event field");
                return;
            }
            String schemaName = event.getSchema().getName();
            if ("RideUpdateABikeEventAvro".equals(schemaName) || "RideUpdateEBikeEventAvro".equals(schemaName)) {
                String userId = event.get("userId").toString();
                int userCredit = (Integer) event.get("userCredit");
                userService.updateUser(new RequestUserUpdateEvent(userId, userCredit));
                logger.info("Processed {} and triggered UserUpdateEvent for userId={}", schemaName, userId);
            }
            // Se serve: gestisci altri eventi ride qui...
        } catch (Exception e) {
            logger.error("Error processing user update", e);
        }
    }
}
