package application.ports;

import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

/**
 * Port representing the REST API for domain operations in the Application.
 */
public interface RestRideServiceAPI {

    /**
     * Starts a ride for a specific user and e-bike.
     *
     * @param userId the ID of the user.
     * @param bikeId the ID of the e-bike.
     * @return a CompletableFuture that completes when the ride is started.
     */
    CompletableFuture<Void> startRide(String userId, String bikeId, String bikeType);

    /**
     * Stops a ride for a specific user.
     *
     * @param userId the ID of the user.
     * @return a CompletableFuture that completes when the ride is stopped.
     */
    CompletableFuture<Void> stopRide(String userId);


    CompletableFuture<Void> handleUserProjectionUpdate(JsonObject userData);
    CompletableFuture<Void> handleBikeProjectionUpdate(JsonObject ebikeData, String bikeType);
}