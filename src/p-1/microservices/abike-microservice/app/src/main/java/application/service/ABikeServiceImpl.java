package application.service;

import application.port.ABikeRepository;
import application.port.ABikeService;
import application.port.SimulationRepository;
import application.port.StationProjectionRepository;
import domain.events.ABikeArrivedToStation;
import domain.events.ABikeCallComplete;
import domain.events.ABikeRequested;
import domain.events.ABikeUpdate;
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

        abikeRepository.findAll().thenAcceptAsync(list -> {
           list.forEach(abike -> {
               eventPublisher.publish(new ABikeUpdate(abike));
           });
        });
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
    public void completeCall(String abikeId, String userId) {
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
        Simulation simulation = new Simulation(abike, destination, Purpose.TO_STATION, eventPublisher, vertx, this.abikeRepository);
        simulationRepository.save(simulation);
        simulation.start().thenAccept(s -> {
            eventPublisher.publish(new ABikeCallComplete(abikeId, userId));
        });
    }


    public CompletableFuture<String> callABike(Destination destination) {
        logger.info("Calling ABike with destination {}", destination);
        return stationRepository.getAll().thenCombine(
            abikeRepository.findAll(),
            (stations, abikes) -> {
                Optional<Station> nearestStationOpt = stations.stream()
                    .sorted(Comparator.comparingDouble(s -> distance(s.location(), destination.position())))
                    .filter(station -> abikes.stream()
                        .anyMatch(abike -> station.dockedBikes().contains(abike.id()) && abike.state() == ABikeState.AVAILABLE))
                    .findFirst();

                if (nearestStationOpt.isEmpty()) {
                    throw new IllegalStateException("No available ABike at any station");
                }
                Station nearestStation = nearestStationOpt.get();

                Optional<ABike> abikeOpt = abikes.stream()
                    .filter(abike -> nearestStation.dockedBikes().contains(abike.id()) && abike.state() == ABikeState.AVAILABLE)
                    .findFirst();

                if (abikeOpt.isEmpty()) {
                    throw new IllegalStateException("No available ABike at nearest station");
                }
                ABike abike = abikeOpt.get();
                eventPublisher.publish(new ABikeRequested(abike.getId(), destination.getId(), nearestStation.getId()));
                Simulation simulation = new Simulation(abike, destination, Purpose.TO_USER, eventPublisher, vertx, this.abikeRepository);
                simulationRepository.save(simulation);
                simulation.start();
                return simulation.id;
            }
        );
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

    @Override
    public CompletableFuture<ABike> updateABike(ABike abike) {
        return abikeRepository.findById(abike.id())
            .thenCompose(existing -> {
                if (existing == null) {
                    logger.warn("ABike with id {} not found for update", abike.id());
                    return CompletableFuture.completedFuture(null);
                }
                ABike updated = new ABike(
                    abike.id(),
                    abike.position(),
                    abike.batteryLevel(),
                    abike.state()
                );
                return abikeRepository.update(updated)
                    .thenApply(v -> {
                        eventPublisher.publish(new ABikeUpdate(updated));
                        return updated;
                    });
            })
            .exceptionally(ex -> {
                logger.error("Error updating ABike {}: {}", abike.id(), ex.getMessage());
                return null;
            });
    }
}