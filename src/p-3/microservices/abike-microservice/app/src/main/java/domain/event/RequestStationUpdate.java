package domain.event;

import domain.model.Station;

import java.time.Instant;
import java.util.UUID;

public record RequestStationUpdate(
        String id,
        Station station,
        String timestamp
) implements Event{

    public RequestStationUpdate(Station station){
        this(UUID.randomUUID().toString(), station, Instant.now().toString());
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
