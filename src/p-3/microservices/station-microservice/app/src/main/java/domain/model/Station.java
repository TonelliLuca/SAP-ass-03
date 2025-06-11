// src/main/java/domain/model/Station.java
package domain.model;

import java.util.HashSet;
import java.util.Set;

public class Station {
    private final String id;
    private final P2d location;
    private final int capacity;
    private final HashSet<String> dockedBikes = new HashSet<>();

    public Station(String id, P2d location, int capacity) {
        this.id = id;
        this.location = location;
        this.capacity = capacity;
    }
    public String getId()         { return id; }
    public P2d getLocation()      { return location; }
    public int getCapacity()      { return capacity; }
    public HashSet<String> getDockedBikes() { return dockedBikes; }
    public int getAvailableCapacity() {return capacity-dockedBikes.size();}
    public void dockBike(String bikeId) {
        if (dockedBikes.size() >= capacity) {
            throw new IllegalStateException("Station full");
        }
        if(!dockedBikes.add(bikeId)) {
            throw new IllegalStateException("Bike is already docked");
        }
    }

    public void releaseBike(String bikeId) {
        if(!dockedBikes.remove(bikeId)) {
            throw new IllegalStateException("Bike not docked here");
        }
    }

    @Override
    public String toString() {
        return String.format("Station{id=%s, location=%s, capacity=%d, docked=%s, available slots=%d}", id, location, capacity, dockedBikes, getAvailableCapacity());
    }

}
