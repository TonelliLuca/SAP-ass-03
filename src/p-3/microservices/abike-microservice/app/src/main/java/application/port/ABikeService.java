package application.port;

import domain.event.Event;
import domain.model.ABike;

import java.util.concurrent.CompletableFuture;

/**
 * ABikeService interface defines the contract for managing ABike-related operations.
 * It provides asynchronous methods for handling events and updating projections.
 */
public interface ABikeService {

    /**
     * Handles the process of calling an ABike based on the provided event.
     *
     * @param event The event containing details for the ABike call.
     * @return A CompletableFuture containing the identifier of the called ABike.
     */
    CompletableFuture<String> callABike(Event event);

    /**
     * Creates a new ABike entity based on the provided event.
     *
     * @param event The event containing details for the ABike creation.
     * @return A CompletableFuture that completes when the ABike creation is done.
     */
    CompletableFuture<Void> createABike(Event event);

    /**
     * Completes the ABike call process based on the provided event.
     *
     * @param event The event containing details for completing the ABike call.
     */
    void completeCall(Event event);

    /**
     * Saves the station projection based on the provided event.
     *
     * @param event The event containing details for saving the station projection.
     * @return A CompletableFuture that completes when the station projection is saved.
     */
    CompletableFuture<Void> saveStationProjection(Event event);

    /**
     * Updates the station projection based on the provided event.
     *
     * @param event The event containing details for updating the station projection.
     * @return A CompletableFuture that completes when the station projection is updated.
     */
    CompletableFuture<Void> updateStationProjection(Event event);

    /**
     * Updates the ABike entity based on the provided event.
     *
     * @param event The event containing details for updating the ABike.
     * @return A CompletableFuture containing the updated ABike entity.
     */
    CompletableFuture<ABike> updateABike(Event event);

    /**
     * Cancels the ABike call process based on the provided event.
     *
     * @param event The event containing details for canceling the ABike call.
     * @return A CompletableFuture that completes when the ABike call is canceled.
     */
    CompletableFuture<Void> cancellCall(Event event);
}
