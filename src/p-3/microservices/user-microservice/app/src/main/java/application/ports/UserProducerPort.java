package application.ports;

import domain.event.Event;


/**
 * Interface for the User Producer Port.
 * Provides methods for sending user-related updates to external systems or services.
 */
public interface UserProducerPort {

    /**
     * Sends an update event to the external system.
     *
     * @param update the event containing the update details
     */
    void sendUpdate(Event update);
}