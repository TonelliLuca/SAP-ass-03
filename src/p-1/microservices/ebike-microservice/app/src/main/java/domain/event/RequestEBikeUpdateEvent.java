package domain.event;

import java.time.Instant;
import java.util.UUID;
import io.vertx.core.json.JsonObject;

public record RequestEBikeUpdateEvent(
    String id,
    String bikeId,
    double bikeX,
    double bikeY,
    String bikeState,
    int bikeBattery,
    String timestamp
) implements Event {
    public RequestEBikeUpdateEvent(String bikeId, double bikeX, double bikeY, String bikeState, int bikeBattery) {
        this(UUID.randomUUID().toString(), bikeId, bikeX, bikeY, bikeState, bikeBattery, Instant.now().toString());
    }



    @Override
    public String getId() { return id; }
    @Override
    public String getTimestamp() { return timestamp; }
}