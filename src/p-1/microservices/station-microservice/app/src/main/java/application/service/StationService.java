// src/main/java/application/service/StationService.java
package application.service;

import application.ports.*;
import domain.events.BikeDockedEvent;
import domain.events.BikeReleasedEvent;
import domain.events.StationRegisteredEvent;
import domain.model.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StationService implements Service {
    private final StationRepository repo;
    private final DomainEventPublisher pub;
    private static final Logger log = LoggerFactory.getLogger(StationService.class);

    public StationService(StationRepository repo, DomainEventPublisher pub) {
        this.repo = repo;
        this.pub  = pub;
    }
    @Override
    public void init() {
        log.info("Initializing stations...");
        repo.getAll().thenAccept(stations -> {
            stations.forEach(station ->
                pub.publish(new StationRegisteredEvent(station.getId(), station.getLocation(), station.getCapacity()))
            );
        });
    }

    @Override
    public void dockBike(String stationId, String bikeId) {
        repo.findById(stationId).thenAccept(opt -> {
            Station s = opt.orElseThrow(() -> new IllegalArgumentException("Unknown station"));
            s.dockBike(bikeId);
            repo.save(s);
            pub.publish(new BikeDockedEvent(stationId, bikeId, s.getAvailableSlots()));
        });
    }

    @Override
    public void releaseBike(String stationId, String bikeId) {
        repo.findById(stationId).thenAccept(opt -> {
            Station s = opt.orElseThrow(() -> new IllegalArgumentException("Unknown station"));
            s.releaseBike(bikeId);
            repo.save(s);
            pub.publish(new BikeReleasedEvent(stationId, bikeId, s.getAvailableSlots()));
        });
    }
}
