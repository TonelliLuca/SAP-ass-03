package domain.event;

import domain.model.User;

public record UserUpdateEvent(
    String id,
    String username,
    int credit,
    String timestamp
) implements Event {
    @Override
    public String getId() { return id; }
    @Override
    public String getTimestamp() { return timestamp; }
}