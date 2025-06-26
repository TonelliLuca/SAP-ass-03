package domain.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BikeReleasedEvent(String id, String abikeId, String stationId, String timestamp) implements Event {
    public BikeReleasedEvent(String abikeId, String stationId){
        this(UUID.randomUUID().toString(), abikeId, stationId, Instant.now().toString());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTimestamp() {
        return timestamp;    }

}
