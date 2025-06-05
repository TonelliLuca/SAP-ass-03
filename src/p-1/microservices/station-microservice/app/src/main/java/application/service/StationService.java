package application.service;

import application.ports.DomainEventPublisher;
import application.ports.Service;
import application.ports.StationRepository;
import domain.events.*;
import domain.model.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class StationService implements Service {
    private final StationRepository stationRepository;
    private final DomainEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(StationService.class);

    public StationService(StationRepository stationRepository, DomainEventPublisher eventPublisher) {
        this.stationRepository = stationRepository;
        this.eventPublisher = eventPublisher;
    }

   @Override
    public void init() {
        log.info("Initializing stations...");
        stationRepository.getAll()
            .thenAcceptAsync(stations -> {
                log.info("Stations have been initialized");
                log.info("Stations found: " + stations.size());
                stations.forEach(station -> {
                    log.debug("Publishing StationRegisteredEvent for station: {}", station.getId());
                    eventPublisher.publish(new StationRegisteredEvent(station));
                });
            })
            .exceptionally(ex -> {
                log.error("Exception during station initialization", ex);
                return null;
            });
    }

    @Override
    public void handleABikeArrivedToStation(Event event) {
        if (event instanceof BikeDockedEvent) {
            String stationId = ((BikeDockedEvent) event).stationId();
            String abikeId = ((BikeDockedEvent) event).bikeId();
            stationRepository.findById(stationId).thenAccept(optStation -> {
                if (optStation.isPresent()) {
                    Station station = optStation.get();
                    try {
                        station.dockBike(abikeId);
                        stationRepository.update(station);
                        // Emit StationUpdateEvent with the updated station
                        eventPublisher.publish(new StationUpdateEvent(station));
                        log.info("Docked abike {} at station {} and published StationUpdateEvent", abikeId, stationId);
                    } catch (Exception e) {
                        log.error("Failed to dock abike: {}", e.getMessage());
                    }
                } else {
                    log.warn("Station {} not found for docking abike {}", stationId, abikeId);
                }
            });
        }
    }

    @Override
    public void handleBikeReleased(Event event) {
        if (event instanceof BikeReleasedEvent releasedEvent) {
            String stationId = releasedEvent.stationId();
            String bikeId = releasedEvent.abikeId();
            stationRepository.findById(stationId).thenAccept(optStation -> {
                if (optStation.isPresent()) {
                    Station station = optStation.get();
                    try {
                        station.releaseBike(bikeId);
                        stationRepository.update(station);
                        eventPublisher.publish(new StationUpdateEvent(station));
                        log.info("Released abike {} from station {} and published StationUpdateEvent", bikeId, stationId);
                    } catch (Exception e) {
                        log.error("Failed to release abike: {}", e.getMessage());
                    }
                } else {
                    log.warn("Station {} not found for releasing abike {}", stationId, bikeId);
                }
            });
        }
    }
}