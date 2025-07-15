package application.port;

import ddd.Repository;
import domain.model.ABike;

import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

/**
 * ABikeRepository interface defines the contract for managing ABike entities in the system.
 * It extends the generic Repository interface and provides asynchronous methods for CRUD operations.
 */
public interface ABikeRepository extends Repository {

    /**
     * Saves a new ABike entity asynchronously.
     *
     * @param ABike The ABike entity to be saved.
     * @return A CompletableFuture that completes when the save operation is done.
     */
    CompletableFuture<Void> save(ABike ABike);

    /**
     * Finds an ABike entity by its unique identifier asynchronously.
     *
     * @param id The unique identifier of the ABike entity.
     * @return A CompletableFuture containing the ABike entity if found, or null if not found.
     */
    CompletableFuture<ABike> findById(String id);

    /**
     * Retrieves all ABike entities asynchronously.
     *
     * @return A CompletableFuture containing a HashSet of all ABike entities.
     */
    CompletableFuture<HashSet<ABike>> findAll();

    /**
     * Updates an existing ABike entity asynchronously.
     *
     * @param ABike The ABike entity with updated information.
     * @return A CompletableFuture that completes when the update operation is done.
     */
    CompletableFuture<Void> update(ABike ABike);
}
