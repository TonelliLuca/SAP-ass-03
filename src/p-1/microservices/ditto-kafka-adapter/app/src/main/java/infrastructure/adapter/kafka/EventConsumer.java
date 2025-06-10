package infrastructure.adapter.kafka;

import application.DittoTranslatorService;
import application.port.DittoTranslatorServicePort;
import domain.model.ABikeUpdateEvent;
import domain.model.StationUpdateEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventConsumer {
    private final GenericKafkaConsumer<ABikeUpdateEvent> abikeConsumer;
    private final GenericKafkaConsumer<StationUpdateEvent> stationConsumer;
    private final GenericKafkaConsumer<String> dittoConsumer;
    private final DittoTranslatorServicePort service;

    public EventConsumer(String bootstrapServer, DittoTranslatorServicePort service) {
        this.abikeConsumer = new GenericKafkaConsumer<>(bootstrapServer, "ditto-translator-abike", "abike-events", ABikeUpdateEvent.class);
        this.stationConsumer = new GenericKafkaConsumer<>(bootstrapServer, "ditto-translator-station", "station-events", StationUpdateEvent.class);
        this.dittoConsumer = new GenericKafkaConsumer<>(bootstrapServer, "ditto-inbox-consumer", "ditto-commands", String.class);

        abikeConsumer.start((key, event) -> service.handleEvent(event));
        stationConsumer.start((key, event) -> service.handleEvent(event));
        dittoConsumer.start(this::handleDittoEvent);
        this.service = service;
    }

    private void handleDittoEvent(String key, String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(message);
            String topic = root.path("topic").asText();
            String[] topicParts = topic.split("/");
            String thingId = topicParts.length > 1 ? topicParts[1] : null;
            String correlationId = root.path("headers").path("correlation-id").asText();
            // Always extract ditto-reply-target, default to "0" if missing
            String replyTarget = root.path("headers").has("ditto-reply-target")
                    ? root.path("headers").path("ditto-reply-target").asText()
                    : "0";
            service.sendDittoResponse(thingId, correlationId, "ok", null, replyTarget);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        abikeConsumer.stop();
        stationConsumer.stop();
        dittoConsumer.stop();
    }
}