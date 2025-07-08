package domain.event;

import domain.model.ABike;

public record ABikeUpdateEvent(String id, ABike abike, String type, String timestamp) {
}
