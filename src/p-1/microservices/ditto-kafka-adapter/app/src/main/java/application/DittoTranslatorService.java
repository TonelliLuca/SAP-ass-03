package application;

import application.port.DittoProducerPort;
import application.port.DittoTranslatorServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import infrastructure.adapter.ditto.DittoEventFactory;

import java.util.Map;

public class DittoTranslatorService implements DittoTranslatorServicePort {
    private final DittoProducerPort producerPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DittoTranslatorService(DittoProducerPort producerPort) {
        this.producerPort = producerPort;
    }

    @Override
    public void handleEvent(Object event) {
        Map<String, Object> dittoMsg = DittoEventFactory.getInstance().toDittoMessage(event);
        String key = extractKey(event);
        try {
            String json = objectMapper.writeValueAsString(dittoMsg);
            producerPort.send(key, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Ditto message", e);
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