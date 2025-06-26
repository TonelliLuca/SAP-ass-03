package application.ports;

import domain.event.Event;

import java.util.concurrent.CompletableFuture;

public interface Service {
    void init();
    void handleABikeArrivedToStation(Event event);
    void handleBikeReleased(Event event);
    CompletableFuture<Void> createStation(Event event);
}
