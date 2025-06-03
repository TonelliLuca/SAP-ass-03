package domain.events;

import domain.model.P2d;

import java.util.Set;

public record StationRegisteredEvent(String stationId, P2d location, int capacity, int availableCapacity, Set<String> dockedBikes) implements Event {}
