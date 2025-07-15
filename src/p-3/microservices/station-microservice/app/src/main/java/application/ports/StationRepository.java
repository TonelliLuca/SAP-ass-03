package application.ports;

    import ddd.Repository;
    import domain.model.Station;

    import java.util.HashMap;
    import java.util.HashSet;
    import java.util.Optional;
    import java.util.concurrent.CompletableFuture;

    /**
     * StationRepository interface defines the contract for managing Station entities.
     * It extends the generic Repository interface and provides asynchronous methods
     * for CRUD operations and retrieval of Station entities.
     */
    public interface StationRepository extends Repository {

        /**
         * Finds a Station entity by its unique identifier asynchronously.
         *
         * @param id The unique identifier of the Station entity.
         * @return A CompletableFuture containing an Optional of the Station entity if found,
         *         or an empty Optional if not found.
         */
        CompletableFuture<Optional<Station>> findById(String id);

        /**
         * Saves a new Station entity asynchronously.
         *
         * @param station The Station entity to be saved.
         * @return A CompletableFuture that completes when the save operation is done.
         */
        CompletableFuture<Void> save(Station station);

        /**
         * Retrieves all Station entities asynchronously.
         *
         * @return A CompletableFuture containing a HashSet of all Station entities.
         */
        CompletableFuture<HashSet<Station>> getAll();

        /**
         * Updates an existing Station entity asynchronously.
         *
         * @param station The Station entity with updated information.
         * @return A CompletableFuture that completes when the update operation is done.
         */
        CompletableFuture<Void> update(Station station);
    }