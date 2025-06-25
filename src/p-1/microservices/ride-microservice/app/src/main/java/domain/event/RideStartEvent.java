package domain.event;

import java.time.Instant;
import java.util.UUID;

public record RideStartEvent(
    String id,
    String username,
    String bikeId,
    String timestamp
) implements Event {
    public RideStartEvent(String username, String bikeId) {
        this(UUID.randomUUID().toString(), username, bikeId, Instant.now().toString());
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