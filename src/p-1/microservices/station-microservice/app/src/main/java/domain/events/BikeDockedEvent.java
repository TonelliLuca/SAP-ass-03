package domain.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BikeDockedEvent(String bikeId, String stationId, long timestamp) implements Event {
}