package application.ports;

import domain.event.Event;
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
    CompletableFuture<Void> startRide(Event event);

    /**
     * Stops a ride for a specific user.
     *
     * @param userId the ID of the user.
     * @return a CompletableFuture that completes when the ride is stopped.
     */
    CompletableFuture<Void> stopRide(Event event);


}