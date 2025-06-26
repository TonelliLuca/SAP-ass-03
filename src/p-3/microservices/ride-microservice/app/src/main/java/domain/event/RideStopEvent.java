package domain.event;

import domain.event.Event;

import java.time.Instant;
import java.util.UUID;

public record RideStopEvent(
        String id,
        String username,
        String bikeId,
        String type,
        String timestamp
) implements Event {
    public RideStopEvent(String username, String bikeId, String type) {
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