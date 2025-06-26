package application.ports;

import domain.event.Event;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

/**
* Interface representing the REST API for domain operations in the Application.
* This API provides methods to start and stop rides for users and e-bikes.
*/
public interface RestRideServiceAPI {

 /**
  * Starts a ride for a specific user and e-bike.
  *
  * @param event the event containing details about the user and e-bike.
  *              The event should include the user ID and bike ID.
  * @return a CompletableFuture that completes when the ride is successfully started.
  */
 CompletableFuture<Void> startRide(Event event);

 /**
  * Stops a ride for a specific user.
  *
  * @param event the event containing details about the user and the ride to be stopped.
  *              The event should include the user ID and ride details.
  * @return a CompletableFuture that completes when the ride is successfully stopped.
  */
 CompletableFuture<Void> stopRide(Event event);

}