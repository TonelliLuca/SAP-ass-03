package domain.events;


public record ABikeCallComplete(String bikeId, String userId) implements Event{
}
