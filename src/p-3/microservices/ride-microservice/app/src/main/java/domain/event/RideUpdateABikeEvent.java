package domain.event;

import java.time.Instant;
import java.util.UUID;

public record RideUpdateABikeEvent(
    String id,
    String rideId,
    String userId,
    int userCredit,
    String bikeId,
    double bikeX,
    double bikeY,
    String bikeState,
    int bikeBattery,
    String timestamp
) implements Event {
    public RideUpdateABikeEvent(
        String rideId,
        String userId,
        int userCredit,
        String bikeId,
        double bikeX,
        double bikeY,
        String bikeState,
        int bikeBattery
    ) {
        this(
            UUID.randomUUID().toString(),
            rideId,
            userId,
            userCredit,
            bikeId,
            bikeX,
            bikeY,
            bikeState,
            bikeBattery,
            Instant.now().toString()
        );
    }

    @Override
    public String getId() { return id; }
    @Override
    public String getTimestamp() { return timestamp; }
}