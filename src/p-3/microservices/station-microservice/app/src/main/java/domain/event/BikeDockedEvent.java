package domain.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BikeDockedEvent(String id, String bikeId, String stationId, String timestamp) implements Event {
    public BikeDockedEvent(String bikeId, String stationId){
        this(UUID.randomUUID().toString(), bikeId, stationId, Instant.now().toString());
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