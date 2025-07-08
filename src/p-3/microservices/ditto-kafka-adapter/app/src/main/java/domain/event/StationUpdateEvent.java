package domain.event;

import domain.model.Station;

public record StationUpdateEvent(String id, Station station, String timestamp) {
}
