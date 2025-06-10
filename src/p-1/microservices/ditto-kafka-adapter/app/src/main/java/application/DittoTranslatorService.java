package application;
import application.port.DittoProducerPort;
import application.port.DittoTranslatorServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.model.DittoEventFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DittoTranslatorService implements DittoTranslatorServicePort {
    private final DittoProducerPort producerPort;
    private final DittoProducerPort dittoResponseProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> knownIds = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(DittoTranslatorService.class);

    public DittoTranslatorService(DittoProducerPort producerPort, DittoProducerPort dittoResponseProducer) {
        this.producerPort = producerPort;
        this.dittoResponseProducer = dittoResponseProducer;
    }

    @Override
    public void handleEvent(Object event) {
        String id = extractKey(event);
        logger.info("handleEvent: " + id);
        if (!knownIds.contains(id)) {
            logger.info("Id not known: " + id);
            knownIds.add(id);
            Map<String, Object> createMsg = DittoEventFactory.getInstance().toDittoCreateMessage(event);
            try {
                String createJson = objectMapper.writeValueAsString(createMsg);
                producerPort.send(id, createJson);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize Ditto create message", e);
            }
        }
        Map<String, Object> dittoMsg = DittoEventFactory.getInstance().toDittoMessage(event);
        try {
            String json = objectMapper.writeValueAsString(dittoMsg);
            logger.info("Ditto message serialized: " + json);
            producerPort.send(id, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Ditto message", e);
        }
    }

    @Override
    public void sendDittoResponse(String thingId, String correlationId, String status, Object payload, String replyTarget) {
        try {
            Map<String, Object> response = DittoEventFactory.getInstance()
                        .toDittoResponseMessage(thingId, correlationId, status, payload);
            String json = objectMapper.writeValueAsString(response);
            dittoResponseProducer.sendWithCorrelationHeader( correlationId, json);
            logger.info("Sent Ditto response: " + json);
        } catch (Exception e) {
            logger.error("Failed to send Ditto response", e);
        }
    }

    private String extractKey(Object event) {
        if (event instanceof domain.model.StationUpdateEvent stationEvent) {
            return stationEvent.station().id();
        } else if (event instanceof domain.model.ABikeUpdateEvent abikeEvent) {
            return abikeEvent.abike().id();
        }
        return "unknown";
    }
}