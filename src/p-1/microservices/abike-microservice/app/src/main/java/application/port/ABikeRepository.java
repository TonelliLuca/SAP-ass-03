package application.port;

import ddd.Repository;
import domain.model.ABike;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

public interface ABikeRepository extends Repository {
    CompletableFuture<Void> save(ABike ABike);
    CompletableFuture<ABike> findById(String id);
    CompletableFuture<HashSet<ABike>> findAll();
}
