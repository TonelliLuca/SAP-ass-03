package domain.events;

public record BikeReleasedEvent(String bikeId, String stationId, long timestamp) {
}
