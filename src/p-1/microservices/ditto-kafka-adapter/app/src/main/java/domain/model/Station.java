package domain.model;
import java.util.List;

public record Station(String id, Location location, int capacity, List<String> dockedBikes, int availableCapacity) {
}
