package application.ports;

/**
 * Port representing an event publisher for e-bike and user updates.
 */
public interface EventPublisher {
    String RIDE_UPDATE  = "ride.update";

    /**
     * Publishes a complete ride update event.
     *
     * This method is used to publish a comprehensive update for a ride,
     * including both e-bike and user details. The update contains the
     * e-bike's location, state, and battery level, as well as the user's
     * username, credit information, and the ride ID.
     *
     * @param id the ID of the e-bike.
     * @param x the x-coordinate of the e-bike's location.
     * @param y the y-coordinate of the e-bike's location.
     * @param state the state of the e-bike.
     * @param batteryLevel the battery level of the e-bike.
     * @param username the username of the user.
     * @param credit the credit of the user.
     * @param rideId the unique identifier of the ride.
     */
    void publishRideUpdate(String id, double x, double y, String state, int batteryLevel, String username, int credit, String rideId);
}