package infrastructure.adapter.persistence;

import application.ports.StationRepository;
import domain.model.Station;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class StationRepositoryImpl implements StationRepository {
    private final List<Station> stations = new CopyOnWriteArrayList<>();

    @Override
    public CompletableFuture<Void> saveStation(Station station) {
        stations.removeIf(s -> s.getId().equals(station.getId()));
        stations.add(station);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<Station>> getAllStations() {
        return CompletableFuture.completedFuture(List.copyOf(stations));
    }
}