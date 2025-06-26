package domain.event;

import java.time.Instant;
import java.util.UUID;

public record UserRequestedAbike(
        String id,
        String username,
        String timestamp
) implements Event{

    public UserRequestedAbike(String userId){
        this(UUID.randomUUID().toString(), userId, Instant.now().toString());
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
