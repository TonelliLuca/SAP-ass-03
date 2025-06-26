package domain.event;

import java.time.Instant;
import java.util.UUID;

public record CancellCallRequest(
        String id,
        String userId,
        String timestamp
) implements Event {

    public CancellCallRequest(String userId){
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
