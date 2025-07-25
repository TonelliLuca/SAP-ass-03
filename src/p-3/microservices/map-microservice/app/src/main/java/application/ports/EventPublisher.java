package application.ports;

import domain.model.ABike;
import domain.model.Bike;
import domain.model.EBike;
import domain.model.Station;

import java.util.List;
import java.util.Map;

/**
 * Port representing an event publisher for e-bike updates and user ride events.
 */
public interface EventPublisher {

    /**
     * Publishes an update for a list of e-bikes.
     *
     * @param bikes the list of e-bikes to update.
     */
    void publishBikesUpdate(List<Bike> bikes);

    /**
     * Publishes an update for a user's e-bikes.
     *
     * @param bikes the list of e-bikes for the user.
     * @param username the username of the user.
     */
    void publishUserBikesUpdate(List<Bike> bikes, String username);

    /**
     * Publishes an update for the available e-bikes of a user.
     *
     * @param bikes the list of available e-bikes for the user.
     */
    void publishUserAvailableBikesUpdate(List<Bike> bikes);

    /**
     * Publishes an event to notify a user that his ride has been forced to stop.
     *
     * @param username the username of the user whose ride is to be stopped.
     */
    void publishStopRide(String username);

    void publishStationsUpdate(List<Station> stations);
    void publishABikeArrivedToUser(String userId, String abikeId);

}