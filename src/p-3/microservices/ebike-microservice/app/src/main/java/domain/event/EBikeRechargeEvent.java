package domain.event;

import java.time.Instant;
import java.util.UUID;

public record EBikeRechargeEvent(
    String id,
    String ebikeId,
    String timestamp
) implements Event {
    public EBikeRechargeEvent(String ebikeId) {
        this(UUID.randomUUID().toString(), ebikeId, Instant.now().toString());
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