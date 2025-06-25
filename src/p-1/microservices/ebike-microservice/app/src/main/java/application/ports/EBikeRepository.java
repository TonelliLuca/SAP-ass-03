package application.ports;

import domain.model.EBike;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Port for the EBike Repository.
 * Provides methods to perform CRUD operations on eBikes.
 */
public interface EBikeRepository {

    /**
     * Saves a new eBike.
     * This method persists the details of a new eBike in the repository.
     *
     * @param ebike the eBike details to save as a JsonObject
     * @return a CompletableFuture that completes when the save operation is done
     */
    CompletableFuture<Void> save(EBike ebike);

    /**
     * Updates an existing eBike.
     * This method modifies the details of an existing eBike in the repository.
     *
     * @param ebike the eBike details to update as a JsonObject
     * @return a CompletableFuture that completes when the update operation is done
     */
    CompletableFuture<Void> update(EBike ebike);

    /**
     * Finds an eBike by its id.
     * This method retrieves an eBike from the repository using its unique identifier.
     *
     * @param id the unique identifier of the eBike
     * @return a CompletableFuture containing an Optional with the eBike as a JsonObject if found, or an empty Optional if not found
     */
    CompletableFuture<Optional<EBike>> findById(String id);

    /**
     * Retrieves all eBikes.
     * This method fetches a list of all eBikes currently stored in the repository.
     *
     * @return a CompletableFuture containing a JsonArray of all eBikes
     */
    CompletableFuture<List<EBike>> findAll();
}