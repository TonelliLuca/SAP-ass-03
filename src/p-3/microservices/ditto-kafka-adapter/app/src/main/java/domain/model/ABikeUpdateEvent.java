package domain.model;

public record ABikeUpdateEvent(String id, ABike abike, String type, String timestamp) {
}
