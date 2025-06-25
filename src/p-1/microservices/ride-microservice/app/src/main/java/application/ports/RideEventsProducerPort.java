package application.ports;

import domain.event.Event;
import io.vertx.core.json.JsonObject;

/**
 * Interface for the Ride Events Producer Port.
 * Provides methods for publishing ride-related events to external systems or services.
 */
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