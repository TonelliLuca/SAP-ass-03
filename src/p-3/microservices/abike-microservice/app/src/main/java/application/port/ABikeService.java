package application.port;

import domain.event.Event;
import domain.model.ABike;
import domain.model.Destination;
import domain.model.Station;

import java.util.concurrent.CompletableFuture;

public interface ABikeService {
    CompletableFuture<String> callABike(Event event);
    CompletableFuture<Void> createABike(Event event);
    void completeCall(Event event);
    CompletableFuture<Void> saveStationProjection(Event event);
    CompletableFuture<Void> updateStationProjection(Event event);
    CompletableFuture<ABike> updateABike(Event event);
    CompletableFuture<Void> cancellCall(Event event);
}
