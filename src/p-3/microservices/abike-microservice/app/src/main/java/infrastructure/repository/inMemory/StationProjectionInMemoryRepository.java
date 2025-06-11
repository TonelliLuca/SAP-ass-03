package infrastructure.repository.inMemory;

import application.port.StationProjectionRepository;
import domain.model.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StationProjectionInMemoryRepository implements StationProjectionRepository {
    private final ConcurrentHashMap<String, Station> storage = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(StationProjectionInMemoryRepository.class);

    @Override
    public CompletableFuture<Void> save(Station station) {
        storage.put(station.getId(), station);
        logger.info("Stored station: {}", station);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Station> findById(String id) {
        return CompletableFuture.completedFuture(storage.get(id));
    }

    @Override
    public CompletableFuture<HashSet<Station>> getAll() {
        return CompletableFuture.completedFuture(new HashSet<>(storage.values()));
    }

    @Override
    public CompletableFuture<Void> update(Station station) {
        storage.put(station.getId(), station);
        logger.info("Updated station: {}", station);
        return CompletableFuture.completedFuture(null);
    }
}