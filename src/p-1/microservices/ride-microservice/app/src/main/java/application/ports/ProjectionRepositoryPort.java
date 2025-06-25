package application.ports;

import domain.event.EBikeUpdateEvent;
import domain.event.UserUpdateEvent;
import domain.model.EBike;
import domain.model.User;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for the Projection Repository Port.
 * Provides methods for appending events and retrieving projections of users and e-bikes.
 */
public interface ProjectionRepositoryPort {

    /**
     * Appends a user update event to the repository.
     *
     * @param event the user update event to be appended
     * @return a CompletableFuture that completes when the event is successfully appended
     */
    CompletableFuture<Void> appendUserEvent(UserUpdateEvent event);

    /**
     * Appends an e-bike update event to the repository.
     *
     * @param event the e-bike update event to be appended
     * @return a CompletableFuture that completes when the event is successfully appended
     */
    CompletableFuture<Void> appendEBikeEvent(EBikeUpdateEvent event);

    /**
     * Retrieves a user projection by their username.
     *
     * @param username the username of the user
     * @return a CompletableFuture containing the user projection associated with the given username
     */
    CompletableFuture<User> getUser(String username);

    /**
     * Retrieves an e-bike projection by its unique identifier.
     *
     * @param bikeId the unique identifier of the e-bike
     * @return a CompletableFuture containing the e-bike projection associated with the given identifier
     */
    CompletableFuture<EBike> getEBike(String bikeId);
}