package application.port;

import domain.event.Event;

public interface EventPublisher {
    void publish(Event event);
}
