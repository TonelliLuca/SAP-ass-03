// src/main/java/infrastructure/repository/InMemoryRepository.java
package infrastructure.repository;

import application.ports.StationRepository;
import domain.model.Station;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

public class InMemoryRepository implements StationRepository {
    private final ConcurrentMap<String, Station> store = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Optional<Station>> findById(String id) {
        return CompletableFuture.completedFuture(Optional.ofNullable(store.get(id)));
    }

    @Override
    public CompletableFuture<Void> save(Station station) {
        store.put(station.getId(), station);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<HashSet<Station>> getAll() {
        return CompletableFuture.completedFuture(new HashSet<>(store.values()));
    }
}
