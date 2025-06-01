package infrastructure.repository.inMemory;

import application.port.SimulationRepository;
import domain.service.Simulation;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SimulationInMemoryRepository implements SimulationRepository {
    private final Map<String, Simulation> storage = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> save(Simulation simulation) {
        storage.put(simulation.id, simulation);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Simulation> getById(String id) {
        Simulation sim = storage.get(id);
        return CompletableFuture.completedFuture(sim);
    }
}