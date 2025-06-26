package application.ports;


import domain.event.Event;

public interface RideEventsProducerPort {

    /**
     * Publishes an update event related to a ride.
     *
     * @param event the event containing ride update details
     */
    void publishUpdate(Event event);

    /**
     * Initializes the Ride Events Producer Port.
     * This method can be used to set up necessary configurations or resources.
     */
    void init();
}