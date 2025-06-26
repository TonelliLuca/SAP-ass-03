package domain.event;

import java.time.Instant;
import java.util.UUID;

public record ABikeRequested(String id, String abikeId, String username, String stationId, String timestamp) implements Event {
    public ABikeRequested(String abikeId, String username, String stationId) {
        this(UUID.randomUUID().toString(), abikeId, username, stationId, Instant.now().toString());
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
