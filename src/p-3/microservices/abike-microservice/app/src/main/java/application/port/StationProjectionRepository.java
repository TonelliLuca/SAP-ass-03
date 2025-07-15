package application.port;

import ddd.Repository;
import domain.model.Station;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * StationProjectionRepository interface defines the contract for managing Station projections.
 * It extends the generic Repository interface and provides asynchronous methods for CRUD operations.
 */
public interface StationProjectionRepository extends Repository {

    /**
     * Saves a new Station projection asynchronously.
     *
     * @param station The Station entity to be saved.
     * @return A CompletableFuture that completes when the save operation is done.
     */
    CompletableFuture<Void> save(Station station);

    /**
     * Finds a Station projection by its unique identifier asynchronously.
     *
     * @param id The unique identifier of the Station entity.
     * @return A CompletableFuture containing the Station entity if found, or null if not found.
     */
    CompletableFuture<Station> findById(String id);

    /**
     * Retrieves all Station projections asynchronously.
     *
     * @return A CompletableFuture containing a HashSet of all Station entities.
     */
    CompletableFuture<HashSet<Station>> getAll();

    /**
     * Updates an existing Station projection asynchronously.
     *
     * @param station The Station entity with updated information.
     * @return A CompletableFuture that completes when the update operation is done.
     */
    CompletableFuture<Void> update(Station station);
}