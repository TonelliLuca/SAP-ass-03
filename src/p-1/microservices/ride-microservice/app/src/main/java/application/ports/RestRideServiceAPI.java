package application.ports;

import domain.event.Event;

import java.util.concurrent.CompletableFuture;

/**
* Port representing the REST API for domain operations in the Application.
* Provides methods for starting and stopping rides for users and e-bikes.
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