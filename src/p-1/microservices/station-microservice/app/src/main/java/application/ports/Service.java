package application.ports;

import domain.events.Event;
public interface Service {
    void init();
    void handleABikeArrivedToStation(Event event);
    void handleBikeReleased(Event event);
}
