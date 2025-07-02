package domain.event;

import domain.model.User;

import java.time.Instant;
import java.util.UUID;

public record UserSignInEvent(
        String id,
        String username,
        String timestamp
) implements Event{
    public UserSignInEvent(String username) {
        this(UUID.randomUUID().toString(), username, Instant.now().toString());
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
