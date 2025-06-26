package domain.event;

import java.time.Instant;
import java.util.UUID;

public record ABikeArrivedToUser(String id, String abikeId, String userId, String timestamp) implements Event {
    public ABikeArrivedToUser(String abikeId, String userId){
        this(UUID.randomUUID().toString(), abikeId, userId, Instant.now().toString());
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
