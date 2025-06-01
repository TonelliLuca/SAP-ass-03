package domain.events;

import domain.model.P2d;

public record StationRegisteredEvent(String stationId, P2d location, int capacity, int availableCapacity) {}
