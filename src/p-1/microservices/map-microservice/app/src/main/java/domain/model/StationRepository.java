package domain.model;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StationRepository {
    CompletableFuture<Void> saveStation(Station station);
    CompletableFuture<List<Station>> getAllStations();
}