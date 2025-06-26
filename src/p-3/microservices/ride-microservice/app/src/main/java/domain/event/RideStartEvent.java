package domain.event;

import java.time.Instant;
import java.util.UUID;

public record RideStartEvent(
    String id,
    String username,
    String bikeId,
    String type,
    String timestamp
) implements Event {
    public RideStartEvent(String username, String bikeId, String type) {
        this(UUID.randomUUID().toString(), username, bikeId, type, Instant.now().toString());
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getTimestamp() {
        return this.timestamp;
    }
}