package domain.events;

public record ABikeArrivedToUser(String abikeId, String userId) implements Event {
}
