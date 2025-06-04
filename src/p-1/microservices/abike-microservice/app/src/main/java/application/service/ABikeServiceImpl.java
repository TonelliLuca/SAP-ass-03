package application.service;

import application.port.ABikeRepository;
import application.port.ABikeService;
import application.port.SimulationRepository;
import application.port.StationProjectionRepository;
import domain.events.ABikeArrivedToStation;
import domain.model.*;
import domain.service.Simulation;
import io.vertx.core.Vertx;
import application.port.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ABikeServiceImpl implements ABikeService {

    private final ABikeRepository abikeRepository;
    private final StationProjectionRepository stationRepository;
    private final SimulationRepository simulationRepository;
    private final EventPublisher eventPublisher;
    private final Vertx vertx;
    private final Logger logger = LoggerFactory.getLogger(ABikeServiceImpl.class);
    public ABikeServiceImpl(
            ABikeRepository abikeRepository,
            StationProjectionRepository stationRepository,
            SimulationRepository simulationRepository,
            EventPublisher eventPublisher,
            Vertx vertx
    ) {
        this.abikeRepository = abikeRepository;
        this.stationRepository = stationRepository;
        this.simulationRepository = simulationRepository;
        this.eventPublisher = eventPublisher;
        this.vertx = vertx;

    }

    @Override
    public CompletableFuture<Void> createABike(String abikeId, String stationId) {
        logger.info("Creating ABike with id {}", abikeId);
        return stationRepository.findById(stationId).thenCompose(station -> {
            if (station == null) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Station not found"));
            }
            ABike abike = new ABike(abikeId, station.location(), 100, ABikeState.AVAILABLE);
            return abikeRepository.save(abike).thenAccept(v -> {
                // Only publish event if save succeeded and station exists
                eventPublisher.publish(new domain.events.ABikeArrivedToStation(abikeId, stationId));
                logger.info("Published ABikeArrivedToStation event for abike {} at station {}", abikeId, stationId);
            });
        });
    }

    @Override
    public void dockABike(String abikeId) {
        logger.info("Docking ABike with id {}", abikeId);
        ABike abike = abikeRepository.findById(abikeId).join();
        if (abike == null) {
            throw new IllegalArgumentException("ABike not found");
        }

        // Find all stations
        HashSet<Station> stations = stationRepository.getAll().join();

        // Find nearest station with available space (capacity 10)
        Optional<Station> nearestStationOpt = stations.stream()
                .filter(station -> station.dockedBikes().size() < station.capacity())
                .min(Comparator.comparingDouble(s -> distance(s.location(), abike.position())));
        if (nearestStationOpt.isEmpty()) {
            throw new IllegalStateException("No available station with free space");
        }
        Station nearestStation = nearestStationOpt.get();

        // Start simulation to move bike to station
        Destination destination = new Destination(nearestStation.location(), nearestStation.id());
        Simulation simulation = new Simulation(abike, destination, Purpose.TO_STATION, eventPublisher, vertx);
        simulationRepository.save(simulation);
        simulation.start();
    }

    @Override
    public String callABike(Destination destination) {
        logger.info("Calling ABike with destination {}", destination);
        // Find nearest station
        HashSet<Station> stations = stationRepository.getAll().join();
        Optional<Station> nearestStationOpt = stations.stream()
                .min(Comparator.comparingDouble(s -> distance(s.location(), destination.position())));
        if (nearestStationOpt.isEmpty()) {
            throw new IllegalStateException("No stations available");
        }
        Station nearestStation = nearestStationOpt.get();

        // Find available ABike at the station (by dockedBikes and AVAILABLE state)
        HashSet<ABike> abikes = abikeRepository.findAll().join();
        Optional<ABike> abikeOpt = abikes.stream()
                .filter(abike -> nearestStation.dockedBikes().contains(abike.id()) && abike.state() == ABikeState.AVAILABLE)
                .findFirst();
        if (abikeOpt.isEmpty()) {
            throw new IllegalStateException("No available ABike at nearest station");
        }
        ABike abike = abikeOpt.get();

        // Start simulation
        Simulation simulation = new Simulation(abike, destination, Purpose.TO_USER, eventPublisher, vertx);
        simulationRepository.save(simulation);
        simulation.start();

        return simulation.id;
    }

    private double distance(P2d a, P2d b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public CompletableFuture<Void> saveStationProjection(Station station) {
        return stationRepository.save(station)
            .thenAccept(v -> logger.info("Saved station projection: {}", station))
            .exceptionally(ex -> {
                logger.error("Failed to save station projection: {}", ex.getMessage());
                return null;
            });
    }

    @Override
    public CompletableFuture<Void> updateStationProjection(Station station) {
        return stationRepository.update(station)
            .thenAccept(v -> logger.info("Updated station projection: {}", station))
            .exceptionally(ex -> {
                logger.error("Failed to update station projection: {}", ex.getMessage());
                return null;
            });
    }
}