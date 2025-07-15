package application.ports;

import domain.event.Event;

/**
 * DomainEventPublisher interface defines the contract for publishing domain events
 * and managing the lifecycle of the publisher.
 * Implementations of this interface are responsible for propagating events
 * to the appropriate handlers or external systems.
 */
public interface DomainEventPublisher {

    /**
     * Publishes a domain event to the event bus or other communication channels.
     *
     * @param event The domain event to be published.
     */
    void publish(Event event);

    /**
     * Closes the publisher and releases any resources associated with it.
     */
    void close();
}