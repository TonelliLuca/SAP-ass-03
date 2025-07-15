package application.ports;

import domain.model.Ride;
import domain.model.RideSimulation;

/**
 * RideRepository interface defines the contract for managing Ride entities and their simulations.
 * It provides methods for adding, removing, and retrieving rides and ride simulations.
 */
public interface RideRepository {

    /**
     * Adds a new Ride entity to the repository.
     *
     * @param ride The Ride entity to be added.
     */
    void addRide(Ride ride);

    /**
     * Removes an existing Ride entity from the repository.
     *
     * @param ride The Ride entity to be removed.
     */
    void removeRide(Ride ride);

    /**
     * Retrieves a Ride entity by its unique identifier.
     *
     * @param rideId The unique identifier of the Ride entity.
     * @return The Ride entity if found, or null if not found.
     */
    Ride getRide(String rideId);

    /**
     * Retrieves a RideSimulation entity by the ride's unique identifier.
     *
     * @param rideId The unique identifier of the Ride entity.
     * @return The RideSimulation entity associated with the given ride ID.
     */
    RideSimulation getRideSimulation(String rideId);

    /**
     * Retrieves a RideSimulation entity by the user's unique identifier.
     *
     * @param userId The unique identifier of the user.
     * @return The RideSimulation entity associated with the given user ID.
     */
    RideSimulation getRideSimulationByUserId(String userId);
}