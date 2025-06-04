package domain.events;

public record ABikeArrivedToStation(String bikeId, String stationId) implements Event {
}
