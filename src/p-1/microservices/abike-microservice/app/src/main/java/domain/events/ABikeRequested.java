package domain.events;

public record ABikeRequested(String abikeId, String username, String stationId) implements Event {
}
