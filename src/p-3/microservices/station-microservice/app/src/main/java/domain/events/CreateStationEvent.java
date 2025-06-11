package domain.events;

public record CreateStationEvent(String stationId, double x, double y, int capacity) implements Event{
}
