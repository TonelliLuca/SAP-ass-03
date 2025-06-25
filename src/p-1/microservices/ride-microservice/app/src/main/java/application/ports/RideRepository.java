package application.ports;

import domain.model.Ride;
import domain.model.RideSimulation;

/**
 * Interface for the Ride Repository.
 * Provides methods to manage rides and ride simulations in the application.
 */
public interface RideRepository {

    /**
     * Adds a new ride to the repository.
     *
     * @param ride the ride to be added
     */
    void addRide(Ride ride);

    /**
     * Removes an existing ride from the repository.
     *
     * @param ride the ride to be removed
     */
    void removeRide(Ride ride);

    /**
     * Retrieves a ride by its unique identifier.
     *
     * @param rideId the unique identifier of the ride
     * @return the ride associated with the given identifier
     */
    Ride getRide(String rideId);

    /**
     * Retrieves the ride simulation associated with a specific ride.
     *
     * @param rideId the unique identifier of the ride
     * @return the ride simulation associated with the given ride identifier
     */
    RideSimulation getRideSimulation(String rideId);

    /**
     * Retrieves the ride simulation associated with a specific user.
     *
     * @param userId the unique identifier of the user
     * @return the ride simulation associated with the given user identifier
     */
    RideSimulation getRideSimulationByUserId(String userId);
}