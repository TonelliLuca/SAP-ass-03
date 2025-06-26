package domain.event;

import java.time.Instant;
import java.util.UUID;

public record ABikeArrivedToStation(String id, String bikeId, String stationId, String timestamp) implements Event {
    public ABikeArrivedToStation (String bikeId, String stationId){
        this(UUID.randomUUID().toString(), bikeId, stationId, Instant.now().toString());
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
