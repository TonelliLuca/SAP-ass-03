package application.port;

    import ddd.Repository;
    import domain.service.Simulation;

    import java.util.List;
    import java.util.concurrent.CompletableFuture;

    /**
     * SimulationRepository interface defines the contract for managing Simulation entities.
     * It extends the generic Repository interface and provides asynchronous methods for CRUD operations.
     */
    public interface SimulationRepository extends Repository {

        /**
         * Saves a new Simulation entity asynchronously.
         *
         * @param simulation The Simulation entity to be saved.
         * @return A CompletableFuture that completes when the save operation is done.
         */
        CompletableFuture<Void> save(Simulation simulation);

        /**
         * Retrieves a Simulation entity by its unique identifier asynchronously.
         *
         * @param id The unique identifier of the Simulation entity.
         * @return A CompletableFuture containing the Simulation entity if found, or null if not found.
         */
        CompletableFuture<Simulation> getById(String id);

        /**
         * Removes a Simulation entity by its unique identifier asynchronously.
         *
         * @param id The unique identifier of the Simulation entity to be removed.
         * @return A CompletableFuture that completes when the remove operation is done.
         */
        CompletableFuture<Void> remove(String id);

        /**
         * Retrieves all Simulation entities asynchronously.
         *
         * @return A CompletableFuture containing a list of all Simulation entities.
         */
        CompletableFuture<List<Simulation>> getAll();
    }