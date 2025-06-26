package domain.event;

import java.time.Instant;
import java.util.UUID;

public record ABikeCreateEvent(
        String id,
        String abikeId,
        String stationId,
        String timestamp
) implements Event {

    public ABikeCreateEvent(String abikeId, String stationId){
        this(UUID.randomUUID().toString(), abikeId, stationId, Instant.now().toString());
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
