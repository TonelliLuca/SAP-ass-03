package domain.event;

import domain.model.Destination;
import domain.model.P2d;

import java.time.Instant;
import java.util.UUID;

public record CallAbikeEvent(
        String id,
        Destination destination,
        String timestamp
) implements Event{

    public CallAbikeEvent(double x, double y, String id) {
        this(UUID.randomUUID().toString(), new Destination(new P2d(x, y), id), Instant.now().toString());
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
