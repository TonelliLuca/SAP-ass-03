package infrastructure.adapter.kafka;

import application.port.DittoTranslatorServicePort;
import domain.model.ABikeUpdateEvent;
import domain.model.StationUpdateEvent;

public class EventConsumer {
    private final GenericKafkaConsumer<ABikeUpdateEvent> abikeConsumer;
    private final GenericKafkaConsumer<StationUpdateEvent> stationConsumer;

    public EventConsumer(String bootstrapServer, DittoTranslatorServicePort service) {
        this.abikeConsumer = new GenericKafkaConsumer<>(bootstrapServer, "ditto-translator-abike", "abike-events", ABikeUpdateEvent.class);
        this.stationConsumer = new GenericKafkaConsumer<>(bootstrapServer, "ditto-translator-station", "station-events", StationUpdateEvent.class);
        abikeConsumer.start((key, event) -> service.handleEvent(event));
        stationConsumer.start((key, event) -> service.handleEvent(event));
    }

    public void stop() {
        abikeConsumer.stop();
        stationConsumer.stop();
    }
}