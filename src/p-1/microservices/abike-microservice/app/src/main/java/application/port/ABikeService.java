package application.port;

import domain.model.ABike;
import domain.model.Destination;
import domain.model.Station;

import java.util.concurrent.CompletableFuture;

public interface ABikeService {
    CompletableFuture<String> callABike(Destination destination);
    CompletableFuture<Void> createABike(String abikeId, String stationId);
    void completeCall(String abikeId, String userId);
    CompletableFuture<Void> saveStationProjection(Station station);
    CompletableFuture<Void> updateStationProjection(Station station);
    CompletableFuture<ABike> updateABike(ABike bikeData);

}
