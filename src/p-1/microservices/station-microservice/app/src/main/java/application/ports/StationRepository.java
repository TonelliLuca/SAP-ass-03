package application.ports;

import domain.model.Station;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StationRepository {
    CompletableFuture<Optional<Station>> findById(String id);
    CompletableFuture<Void> save(Station station);
    CompletableFuture<HashSet<Station>> getAll();
}
