package domain.model;
import ddd.Factory;
import domain.event.ABikeUpdateEvent;
import domain.event.StationUpdateEvent;
import jdk.jfr.EventFactory;

import java.util.HashMap;
import java.util.Map;

public class DittoEventFactory implements Factory {

    private static final DittoEventFactory INSTANCE = new DittoEventFactory();

    private DittoEventFactory() {}

    public static DittoEventFactory getInstance() {
        return INSTANCE;
    }

    public Map<String, Object> toDittoMessage(Object event) {
        if (event instanceof StationUpdateEvent stationEvent) {
            return toDittoStation(stationEvent);
        } else if (event instanceof ABikeUpdateEvent abikeEvent) {
            return toDittoAbike(abikeEvent);
        } else {
            throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
        }
    }

    private Map<String, Object> toDittoStation(StationUpdateEvent event) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("topic", "org.eclipse.ditto/" + event.station().id() + "/things/twin/commands/modify");
        msg.put("headers", new HashMap<>());
        msg.put("path", "/features");
        Map<String, Object> value = new HashMap<>();
        Map<String, Object> station = new HashMap<>();
        station.put("properties", event.station());
        value.put("station", station);
        msg.put("value", value);
        return msg;
    }

    private Map<String, Object> toDittoAbike(ABikeUpdateEvent event) {
        String abikeId = event.abike().id();
        Map<String, Object> msg = new HashMap<>();
        msg.put("topic", "org.eclipse.ditto/" + abikeId + "/things/twin/commands/modify");
        msg.put("headers", new HashMap<>());
        msg.put("path", "/features");
        Map<String, Object> value = new HashMap<>();
        Map<String, Object> abike = new HashMap<>();
        abike.put("properties", event.abike());
        value.put("abike", abike);
        msg.put("value", value);
        return msg;
    }

    public Map<String, Object> toDittoCreateMessage(Object event) {
        if (event instanceof StationUpdateEvent stationEvent) {
            return toDittoCreateStation(stationEvent);
        } else if (event instanceof ABikeUpdateEvent abikeEvent) {
            return toDittoCreateAbike(abikeEvent);
        } else {
            throw new IllegalArgumentException("Unsupported event type: " + event.getClass());
        }
    }

    private Map<String, Object> toDittoCreateStation(StationUpdateEvent event) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("topic", "org.eclipse.ditto/" + event.station().id() + "/things/twin/commands/create");
        msg.put("headers", new HashMap<>());
        msg.put("path", "/");
        Map<String, Object> value = new HashMap<>();
        value.put("policyId", "org.eclipse.ditto:default-policy"); // <-- use your existing policy ID
        value.put("features", Map.of("station", Map.of("properties", event.station())));
        msg.put("value", value);
        return msg;
    }

    private Map<String, Object> toDittoCreateAbike(ABikeUpdateEvent event) {
        String abikeId = event.abike().id();
        Map<String, Object> msg = new HashMap<>();
        msg.put("topic", "org.eclipse.ditto/" + abikeId + "/things/twin/commands/create");
        msg.put("headers", new HashMap<>());
        msg.put("path", "/");
        Map<String, Object> value = new HashMap<>();
        value.put("policyId", "org.eclipse.ditto:default-policy"); // <-- use your existing policy ID
        value.put("features", Map.of("abike", Map.of("properties", event.abike())));
        msg.put("value", value);
        return msg;
    }


    public Map<String, Object> toDittoResponseMessage(
            String thingId,
            String correlationId,
            String status,
            Object payload
    ) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "response");
        msg.put("status", 200);

        Map<String, Object> headers = new HashMap<>();
        headers.put("correlation-id", correlationId);
        msg.put("headers", headers);

        msg.put("path", "/outbox/messages/response");

        Map<String, Object> value = new HashMap<>();
        value.put("payload", payload);
        msg.put("value", value);

        return msg;
    }
}