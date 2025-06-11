package domain.events;

import domain.model.Station;

public record StationUpdateEvent(Station station) implements Event {
}
