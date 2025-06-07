package infrastructure.adapter.ditto;

import domain.model.StationUpdateEvent;
import domain.model.ABikeUpdateEvent;

import java.util.HashMap;
import java.util.Map;

public class DittoEventFactory {

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
}