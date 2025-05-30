package application.ports;

public interface DomainEventPublisher {
    void publish(Object event);
    void close();
}
