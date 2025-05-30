// src/main/java/domain/model/Station.java
package domain.model;

import java.util.HashSet;
import java.util.Set;

public class Station {
    private final String id;
    private final P2d location;
    private final int capacity;

    public Station(String id, P2d location, int capacity) {
        this.id = id;
        this.location = location;
        this.capacity = capacity;
    }
    public String getId()         { return id; }
    public P2d getLocation()      { return location; }
    public int getCapacity()      { return capacity; }

}
