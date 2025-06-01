package application.port;

import ddd.Repository;
import domain.service.Simulation;

import java.util.concurrent.CompletableFuture;

public interface SimulationRepository extends Repository {
    CompletableFuture<Void> save(Simulation simulation);
    CompletableFuture<Simulation> getById(String id);
}
