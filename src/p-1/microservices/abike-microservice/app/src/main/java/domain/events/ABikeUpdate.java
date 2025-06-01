package domain.events;

import domain.model.ABike;

public record ABikeUpdate(ABike abike) implements Event {
}
