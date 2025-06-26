package domain.event;


import java.time.Instant;
import java.util.UUID;

public record ABikeCallComplete(String id, String bikeId, String userId, String timestamp) implements Event{
    public ABikeCallComplete(String bikeId, String userId){
        this(UUID.randomUUID().toString(), bikeId, userId, Instant.now().toString());
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
