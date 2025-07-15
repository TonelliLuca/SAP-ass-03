package application.ports;

import domain.event.ABikeUpdateEvent;
import domain.event.EBikeUpdateEvent;
import domain.event.UserUpdateEvent;
import domain.model.Bike;
import domain.model.User;

import java.util.concurrent.CompletableFuture;

/**
 * ProjectionRepositoryPort interface defines the contract for managing projections
 * related to users and bikes. It provides asynchronous methods for appending events
 * and retrieving entities.
 */
public interface ProjectionRepositoryPort {

    /**
     * Appends a user-related update event to the repository asynchronously.
     *
     * @param event The UserUpdateEvent containing details of the user update.
     * @return A CompletableFuture that completes when the event is appended.
     */
    CompletableFuture<Void> appendUserEvent(UserUpdateEvent event);

    /**
     * Appends an e-bike-related update event to the repository asynchronously.
     *
     * @param event The EBikeUpdateEvent containing details of the e-bike update.
     * @return A CompletableFuture that completes when the event is appended.
     */
    CompletableFuture<Void> appendEBikeEvent(EBikeUpdateEvent event);

    /**
     * Appends an a-bike-related update event to the repository asynchronously.
     *
     * @param event The ABikeUpdateEvent containing details of the a-bike update.
     * @return A CompletableFuture that completes when the event is appended.
     */
    CompletableFuture<Void> appendABikeEvent(ABikeUpdateEvent event);

    /**
     * Retrieves a bike entity by its unique identifier and type asynchronously.
     *
     * @param id The unique identifier of the bike.
     * @param bikeType The type of the bike (e.g., e-bike, a-bike).
     * @return A CompletableFuture containing the Bike entity if found, or null if not found.
     */
    CompletableFuture<Bike> getBike(String id, String bikeType);

    /**
     * Retrieves a user entity by their username asynchronously.
     *
     * @param username The username of the user.
     * @return A CompletableFuture containing the User entity if found, or null if not found.
     */
    CompletableFuture<User> getUser(String username);
}