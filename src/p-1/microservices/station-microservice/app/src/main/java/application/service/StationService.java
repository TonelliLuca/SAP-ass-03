// src/main/java/application/service/StationService.java
package application.service;

import application.ports.*;
import domain.events.StationRegisteredEvent;
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

}
