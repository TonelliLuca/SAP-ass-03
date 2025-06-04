package application.service;

import application.ports.DomainEventPublisher;
import application.ports.Service;
import application.ports.StationRepository;
import domain.events.BikeDockedEvent;
import domain.events.Event;
import domain.events.StationRegisteredEvent;
import domain.events.StationUpdateEvent;
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
        stationRepository.getAll().thenAccept(stations -> {
            stations.forEach(station ->
                    eventPublisher.publish(new StationRegisteredEvent(station))
            );
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
}