package domain.events;

public record BikeDockedEvent(String bikeId, String stationId, long timestamp) {
}
