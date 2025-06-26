package domain.event;

import domain.model.ABike;

import java.time.Instant;
import java.util.UUID;

public record ABikeUpdate(String id, ABike abike, String timestamp) implements Event {
    public ABikeUpdate(ABike abike) {
        this(UUID.randomUUID().toString(), abike, Instant.now().toString());
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
