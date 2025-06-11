package domain.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BikeReleasedEvent(String abikeId, String stationId, long timestamp) implements Event {
}
