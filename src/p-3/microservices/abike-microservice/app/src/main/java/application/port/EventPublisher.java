package application.port;

import domain.event.Event;

/**
 * EventPublisher interface defines the contract for publishing domain events.
 * Implementations of this interface are responsible for propagating events
 * to the appropriate handlers or external systems.
 */
public interface EventPublisher {

    /**
     * Publishes a domain event to the event bus or other communication channels.
     *
     * @param event The domain event to be published.
     */
    void publish(Event event);
}
