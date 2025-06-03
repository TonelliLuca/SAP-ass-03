package infrastructure.repository.inMemory;

import application.port.SimulationRepository;
import domain.service.Simulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SimulationInMemoryRepository implements SimulationRepository {
    private final Map<String, Simulation> storage = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(SimulationInMemoryRepository.class);

    @Override
    public CompletableFuture<Void> save(Simulation simulation) {
        logger.info("save simulation {}", simulation);
        storage.put(simulation.id, simulation);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Simulation> getById(String id) {
        Simulation sim = storage.get(id);
        return CompletableFuture.completedFuture(sim);
    }
}