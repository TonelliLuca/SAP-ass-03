package application.port;

import ddd.Repository;
import domain.model.Station;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

public interface StationProjectionRepository extends Repository {
    CompletableFuture<Void> save(Station station);
    CompletableFuture<Station> findById(String id);
    CompletableFuture<HashSet<Station>> getAll();
}
