package application.ports;

import domain.event.Event;

/**
 * Port representing an event publisher for e-bike and user updates.
 * Provides functionality to publish ride update events containing detailed information
 * about e-bike and user states during a ride.
 */
public interface EventPublisher {

    /**
     * Constant representing the topic for ride update events.
     * This topic is used to categorize and identify ride update events in the system.
     */
    String RIDE_UPDATE = "ride.update";

    /**
     * Publishes a ride update event.
     * This method is used to publish an event containing information about the state of a ride,
     * including details about e-bike and user updates.
     *
     * @param event the event containing ride update details
     */
    void publishUpdate(Event event);
}