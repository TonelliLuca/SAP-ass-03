package domain.event;

import java.time.Instant;
import java.util.UUID;

public record CreateStationEvent(String id, String stationId, double x, double y, int capacity, String timestamp) implements Event{

    public CreateStationEvent(String stationId, double x, double y, int capacity){
        this(UUID.randomUUID().toString(), stationId, x, y, capacity, Instant.now().toString());
    }
    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }
}
