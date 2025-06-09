package application.port;

import ddd.Repository;
import domain.service.Simulation;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SimulationRepository extends Repository {
    CompletableFuture<Void> save(Simulation simulation);
    CompletableFuture<Simulation> getById(String id);
    CompletableFuture<Void> remove(String id);
    CompletableFuture<List<Simulation>> getAll();
}
