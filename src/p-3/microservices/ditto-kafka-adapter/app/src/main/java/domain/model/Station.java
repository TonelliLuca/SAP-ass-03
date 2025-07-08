package domain.model;
import ddd.Aggregate;

import java.util.List;

public record Station(String id, Location location, int capacity, List<String> dockedBikes, int availableCapacity) implements Aggregate<String> {
    @Override
    public String getId() {
        return id;
    }
}
