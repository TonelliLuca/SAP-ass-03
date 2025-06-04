package domain.service;

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

    public Simulation(ABike abike, Destination destination, Purpose purpose, EventPublisher eventPublisher, Vertx vertx) {
        this.abike = abike;
        this.destination = destination;
        this.publisher = eventPublisher;
        this.vertx = vertx;
        this.purpose = purpose;
        this.id = UUID.randomUUID().toString();
    }

    public CompletableFuture<Void> start() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.abike = new ABike(abike.id(), abike.position(), abike.batteryLevel(), ABikeState.AUTONOMOUS_MOVING, abike.stationId());
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
            ABikeState newState = ABikeState.AVAILABLE;
            if (purpose == Purpose.TO_STATION && abike.state() == ABikeState.MAINTENANCE) {
                newBattery = 100; // recharge
            }
            abike = new ABike(abike.id(), destination.position(), newBattery, newState, abike.stationId());
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
        abike = new ABike(abike.id(), newPosition, abike.batteryLevel(), abike.state(), abike.stationId());

        publisher.publish(new ABikeUpdate(abike));
    }

    public void stop() {
        stopped = true;
    }

}
