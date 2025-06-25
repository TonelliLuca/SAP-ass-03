package application.ports;

import domain.event.Event;
import domain.model.EBike;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
* Port for the EBike Service API Application.
* Provides methods to manage eBike-related operations in the domain.
*/
public interface EBikeServiceAPI {

    /**
    * Creates a new eBike with the given id and location.
    * This method initializes an eBike in the system with its unique identifier and coordinates.
    *
    * @param event the event containing eBike creation details, including id and location
    * @return a CompletableFuture containing the created eBike as a JsonObject
    */
    CompletableFuture<EBike> createEBike(Event event);

    /**
    * Recharges the battery of an eBike to 100% and sets its state to AVAILABLE.
    * This method updates the battery level and availability status of the eBike.
    *
    * @param event the event containing eBike recharge details, including the eBike id
    * @return a CompletableFuture containing the updated eBike as a JsonObject
    */
    CompletableFuture<EBike> rechargeEBike(Event event);

    /**
    * Updates the details of an existing eBike.
    * This method modifies the attributes of an eBike based on the provided event data.
    *
    * @param event the event containing updated eBike details
    * @return a CompletableFuture containing the updated eBike as a JsonObject
    */
    CompletableFuture<EBike> updateEBike(Event event);

    /**
    * Retrieves all eBikes.
    * This method fetches a list of all eBikes currently available in the system.
    *
    * @return a CompletableFuture containing a JsonArray of all eBikes
    */
    CompletableFuture<List<EBike>> getAllEBikes();
}