package domain.event;

import java.time.Instant;
import java.util.UUID;

public record EBikeCreateEvent(
    String id,
    String ebikeId,
    float x,
    float y,
    String timestamp
) implements Event {
    public EBikeCreateEvent(String ebikeId, float x, float y) {
        this(UUID.randomUUID().toString(), ebikeId, x, y, Instant.now().toString());
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