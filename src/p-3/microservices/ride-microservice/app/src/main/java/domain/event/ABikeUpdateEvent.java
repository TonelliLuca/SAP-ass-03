package domain.event;

import domain.model.BikeState;
import domain.model.P2d;

public record ABikeUpdateEvent(
        String id,
        String bikeId,
        BikeState state,
        P2d location,
        int  batteryLevel,
        String timestamp
) implements Event {
    @Override
    public String getId() { return id; }
    @Override
    public String getTimestamp() { return timestamp; }
}
