package domain.event;

import domain.model.EBike;

import java.time.Instant;
import java.util.UUID;

public record EBikeUpdateEvent(
    String id,
    EBike ebike,
    String timestamp
) implements Event {
    public EBikeUpdateEvent(EBike ebike) {
        this(UUID.randomUUID().toString(), ebike, Instant.now().toString());
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