package application.port;

import domain.model.ABike;
import domain.model.Destination;

import java.util.concurrent.CompletableFuture;

public interface ABikeService {
    String callABike(Destination destination);
    CompletableFuture<Void> createABike(String abikeId, String stationId);
    void dockABike(String abikeId);
}
