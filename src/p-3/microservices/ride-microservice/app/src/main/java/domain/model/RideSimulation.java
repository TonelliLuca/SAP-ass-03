package domain.model;

import application.ports.EventPublisher;
import ddd.Service;
import domain.event.RideUpdateABikeEvent;
import domain.event.RideUpdateEBikeEvent;
import io.vertx.core.Vertx;


import java.util.concurrent.CompletableFuture;

public class RideSimulation implements Service {
    private final Ride ride;
    private final Vertx vertx;
    private volatile boolean stopped = false;
    private long lastTimeChangedDir = System.currentTimeMillis();
    private final EventPublisher publisher;
    private static final int CREDIT_DECREASE = 1;
    private static final int BATTERY_DECREASE = 1;
    private final String id;

    public RideSimulation(Ride ride, Vertx vertx, EventPublisher publisher) {
        this.ride = ride;
        this.vertx = vertx;
        this.publisher = publisher;
        this.id = ride.getId();
    }

    public Ride getRide() {
        return ride;
    }

    public CompletableFuture<Void> startSimulation() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        ride.start();

        if (ride.isOngoing()) {
            vertx.setPeriodic(100, timerId -> {
                if (stopped) {
                    vertx.cancelTimer(timerId);
                    future.complete(null);
                    completeSimulation();
                    return;
                }

                updateRide();
            });
        } else {
            future.complete(null);
        }

        return future;
    }

    private void updateRide() {
        Bike bike = ride.getBike();
        User user = ride.getUser();

        synchronized (bike) {
            if (bike.getBatteryLevel() == 0) {
                System.out.println("Bike has no battery");
                ride.end();
                stopSimulation();
                completeSimulation();
                return;
            }

            if (user.getCredit() == 0) {
                ride.end();
                stopSimulation();
                bike.setState(BikeState.AVAILABLE);
                completeSimulation();
                return;
            }

            V2d direction = bike.getDirection();
            double speed = 0.5;  // Set speed to a constant value for simplicity
            V2d movement = direction.mul(speed);
            bike.setLocation(bike.getLocation().sum(movement));

            if (bike.getLocation().x() > 200 || bike.getLocation().x() < -200) {
                bike.setDirection(new V2d(-direction.x(), direction.y()));
            }
            if (bike.getLocation().y() > 200 || bike.getLocation().y() < -200) {
                bike.setDirection(new V2d(direction.x(), -direction.y()));
            }

            long elapsedTimeSinceLastChangeDir = System.currentTimeMillis() - lastTimeChangedDir;
            if (elapsedTimeSinceLastChangeDir > 500) {
                double angle = Math.random() * 60 - 30;
                bike.setDirection(direction.rotate(angle));
                lastTimeChangedDir = System.currentTimeMillis();
            }

            bike.decreaseBattery(BATTERY_DECREASE);
            user.decreaseCredit(CREDIT_DECREASE);
            if ("abike".equals(ride.getBike().getType())) {
                   publisher.publishUpdate(new RideUpdateABikeEvent(
                       ride.getId(), user.getId(), user.getCredit(), bike.getId(),
                       bike.getLocation().x(), bike.getLocation().y(), bike.getState().name(), bike.getBatteryLevel()
                   ));
               } else {
                   publisher.publishUpdate(new RideUpdateEBikeEvent(
                       ride.getId(), user.getId(), user.getCredit(), bike.getId(),
                       bike.getLocation().x(), bike.getLocation().y(), bike.getState().name(), bike.getBatteryLevel()
                   ));
               }
        }
    }

    private void completeSimulation() {
        if ("abike".equals(ride.getBike().getType())) {
            publisher.publishUpdate(new RideUpdateABikeEvent(
                    ride.getId(), ride.getUser().getId(), ride.getUser().getCredit(), ride.getBike().getId(), ride.getBike().getLocation().x(), ride.getBike().getLocation().y(), ride.getBike().getState().name(), ride.getBike().getBatteryLevel()
            ));
        } else {
            publisher.publishUpdate(new RideUpdateEBikeEvent(
                    ride.getId(), ride.getUser().getId(), ride.getUser().getCredit(), ride.getBike().getId(), ride.getBike().getLocation().x(), ride.getBike().getLocation().y(), ride.getBike().getState().name(), ride.getBike().getBatteryLevel()
            ));
        }
    }

    public void stopSimulation() {
        System.out.println("Stopping simulation " + stopped);
        stopped = true;
    }

    public void stopSimulationManually(){
        System.out.println("Stopping simulation manually");
        ride.end();
        if(ride.getBike().getState() == BikeState.IN_USE){
            ride.getBike().setState(BikeState.AVAILABLE);
        }
        stopped = true;
    }

    public String getId() {
        return id;
    }
}