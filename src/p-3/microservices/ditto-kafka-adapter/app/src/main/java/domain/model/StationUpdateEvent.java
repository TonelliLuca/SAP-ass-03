package domain.model;

public record StationUpdateEvent(String id, Station station, String timestamp) {
}
