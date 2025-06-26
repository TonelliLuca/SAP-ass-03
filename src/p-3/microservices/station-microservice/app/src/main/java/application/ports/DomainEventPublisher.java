package application.ports;

import domain.event.Event;

public interface DomainEventPublisher {
    void publish(Event event);
    void close();
}
