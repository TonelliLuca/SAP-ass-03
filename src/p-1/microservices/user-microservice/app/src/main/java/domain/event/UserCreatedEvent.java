package domain.event;

import domain.model.User;
import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(
    String id,
    String username,
    User.UserType type,
    String timestamp
) implements Event {
    public UserCreatedEvent(String username, String type) {
        this(UUID.randomUUID().toString(), username, User.UserType.valueOf(type), Instant.now().toString());
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