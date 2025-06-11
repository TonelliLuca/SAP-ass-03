package domain.model;

public record ABikeUpdateEvent(ABike abike, String type, long timestamp) {
}
