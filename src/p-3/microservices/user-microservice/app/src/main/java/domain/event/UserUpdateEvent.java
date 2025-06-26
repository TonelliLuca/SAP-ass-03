package domain.event;

import domain.model.User;
import java.time.Instant;
import java.util.UUID;

public record UserUpdateEvent(
    String id,
    User user,
    String timestamp
) implements Event {
    public UserUpdateEvent(User user) {
        this(UUID.randomUUID().toString(), user, Instant.now().toString());
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