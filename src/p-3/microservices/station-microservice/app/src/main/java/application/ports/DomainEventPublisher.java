package application.ports;

import domain.events.Event;

public interface DomainEventPublisher {
    void publish(Event event);
    void close();
}
