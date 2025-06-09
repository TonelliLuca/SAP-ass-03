package domain.service;

import application.port.ABikeRepository;
import application.port.EventPublisher;
import ddd.Service;
import domain.events.ABikeArrivedToStation;
import domain.events.ABikeArrivedToUser;
import domain.events.ABikeUpdate;
import domain.model.*;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Simulation implements Service {
    private ABike abike;
    private final Destination destination;
    public final String id;
    private volatile boolean stopped  = false;
    private static final double SPEED = 1.0;
    private final Purpose purpose;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final EventPublisher publisher;
    private final Vertx vertx;
    private final ABikeRepository abikeRepository;

    public Simulation(ABike abike, Destination destination, Purpose purpose, EventPublisher eventPublisher, Vertx vertx, ABikeRepository repository) {
        this.abike = abike;
        this.destination = destination;
        this.publisher = eventPublisher;
        this.vertx = vertx;
        this.purpose = purpose;
        this.id = UUID.randomUUID().toString();
        this.abikeRepository = repository;
    }

    public CompletableFuture<Void> start() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.abike = new ABike(abike.id(), abike.position(), abike.batteryLevel(), ABikeState.AUTONOMOUS_MOVING);
        logger.info("Starting Simulation");
        vertx.setPeriodic(100, l -> {
            synchronized (this) {
                if (stopped) {
                    vertx.cancelTimer(l);
                    future.complete(null);
                    return;
                }
                this.tick();
            }
        });
        return future;
    }

    private void tick() {
        P2d current = abike.position();
        double dx = destination.position().x() - current.x();
        double dy = destination.position().y() - current.y();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < SPEED) {
            int newBattery = abike.batteryLevel();
            ABikeState newState = abike.state();
            if (purpose == Purpose.TO_STATION) {
                newBattery = 100; // recharge
                newState = ABikeState.AVAILABLE;
            }
            abike = new ABike(abike.id(), destination.position(), newBattery, newState);
            abikeRepository.update(abike); // Persist new position
            publisher.publish(new ABikeUpdate(abike));
            if(purpose == Purpose.TO_USER)
                publisher.publish(new ABikeArrivedToUser(abike.getId(), destination.getId()));
            if(purpose == Purpose.TO_STATION)
                publisher.publish(new ABikeArrivedToStation(abike.getId(), destination.getId()));
            stop();
            return;
        }

        double stepX = (dx / distance) * SPEED;
        double stepY = (dy / distance) * SPEED;
        P2d newPosition = new P2d(current.x() + stepX, current.y() + stepY);
        abike = new ABike(abike.id(), newPosition, abike.batteryLevel(), abike.state());
        abikeRepository.update(abike); // Persist new position
        publisher.publish(new ABikeUpdate(abike));
    }

    public Destination getDestination() {
        return destination;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public String getAbikeId() {
        return abike.getId();
    }

    public void stop() {
        stopped = true;
    }

}
