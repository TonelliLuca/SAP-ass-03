package domain.event;

import domain.model.EBike;
import domain.model.EBikeState;
import domain.model.P2d;

public record EBikeUpdateEvent(
        String id,
        String bikeId,
        EBikeState state,
        P2d location,
        int  batteryLevel,
        String timestamp
) implements Event {
    @Override
    public String getId() { return id; }
    @Override
    public String getTimestamp() { return timestamp; }
}
