package application.port;

import domain.model.ABike;
import domain.model.Destination;
import domain.model.Station;

import java.util.concurrent.CompletableFuture;

public interface ABikeService {
    String callABike(Destination destination);
    CompletableFuture<Void> createABike(String abikeId, String stationId);
    void dockABike(String abikeId);
    CompletableFuture<Void> saveStationProjection(Station station);
    CompletableFuture<Void> updateStationProjection(Station station);
}
