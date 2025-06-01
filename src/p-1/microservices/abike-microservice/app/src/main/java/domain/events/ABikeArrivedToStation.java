package domain.events;

public record ABikeArrivedToStation(String abikeId, String stationId) implements Event {
}
