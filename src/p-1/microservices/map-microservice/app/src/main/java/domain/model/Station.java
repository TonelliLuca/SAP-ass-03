package domain.model;

import ddd.Entity;

public record Station(String stationId, P2d location, int capacity, int availableCapacity) implements Entity<String> {
    @Override
    public String getId() {
        return stationId;
    }

    public P2d getLocation() {
        return location;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getAvailableCapacity() {return availableCapacity;}
}