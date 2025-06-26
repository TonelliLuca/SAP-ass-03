package domain.event;

import domain.model.User;
import java.time.Instant;
import java.util.UUID;

public record RequestUserUpdateEvent(
        String id,
        String username,
        int credit,
        String timestamp
) implements Event {
    public RequestUserUpdateEvent(String username, int credit) {
        this(UUID.randomUUID().toString(), username, credit, Instant.now().toString());
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
