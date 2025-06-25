package domain.event;

import java.time.Instant;
import java.util.UUID;

public record RechargeCreditEvent(
    String id,
    String username,
    int amount,
    String timestamp
) implements Event {
    public RechargeCreditEvent(String username, int amount) {
        this(UUID.randomUUID().toString(), username, amount, Instant.now().toString());
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