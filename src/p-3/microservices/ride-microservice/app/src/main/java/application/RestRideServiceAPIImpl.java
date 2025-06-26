package application;

import application.ports.*;
import domain.event.Event;
import domain.event.RequestRideEndEvent;
import domain.event.RideStartEvent;
import domain.event.RideStopEvent;
import domain.model.*;
import infrastructure.repository.RideRepositoryImpl;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class RestRideServiceAPIImpl implements RestRideServiceAPI {
    private static final Logger logger = LoggerFactory.getLogger(RestRideServiceAPIImpl.class);

    private final RideRepository rideRepository;
    private final ProjectionRepositoryPort projectionRepository;
    private final RideEventsProducerPort rideEventsProducer;

    public RestRideServiceAPIImpl(
            EventPublisher publisher,
            Vertx vertx,
            ProjectionRepositoryPort projectionRepository,
            RideEventsProducerPort rideEventsProducer) {

        this.rideRepository = new RideRepositoryImpl(vertx, publisher);
        this.projectionRepository = projectionRepository;
        this.rideEventsProducer = rideEventsProducer;

        logger.info("RestRideServiceAPIImpl initialized");
    }


    @Override
    public CompletableFuture<Void> startRide(Event event) {
        if(!(event instanceof RideStartEvent rideStartEvent)) {
            logger.error("Invalid event type");
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid event type"));
        }

        String userId = rideStartEvent.username();
        String bikeId = rideStartEvent.bikeId();
        String bikeType = rideStartEvent.type();

        logger.info("Starting ride for user: {} and bike: {}", userId, bikeId);

        CompletableFuture<Bike> bikeFuture = projectionRepository.getBike(bikeId, bikeType);
        CompletableFuture<User> userFuture = projectionRepository.getUser(userId);

        return CompletableFuture.allOf(bikeFuture, userFuture)
            .thenCompose(v -> {
                Bike bike = bikeFuture.join();
                User user = userFuture.join();

                if (bike == null) {
                    logger.error("Bike not found in projection: {}", bikeId);
                    return CompletableFuture.failedFuture(new RuntimeException("Bike not found"));
                }

                if (user == null) {
                    logger.error("User not found in projection: {}", userId);
                    return CompletableFuture.failedFuture(new RuntimeException("User not found"));
                }

                // Generic state check based on bike type
                if ("ebike".equalsIgnoreCase(bike.getType())) {
                    if (bike.getState() != BikeState.AVAILABLE ) {
                        logger.error("Bike is not available: {}, state: {}", bikeId, bike.getState());
                        return CompletableFuture.failedFuture(new RuntimeException("EBike is not available"));
                    }
                } else if ("abike".equalsIgnoreCase(bike.getType())) {
                    if (bike.getState() != BikeState.AUTONOMOUS_MOVING) {
                        logger.error("ABike is not in AUTHONOMOUS_MOVING state: {}, state: {}", bikeId, bike.getState());
                        return CompletableFuture.failedFuture(new RuntimeException("ABike is not in AUTHONOMOUS_MOVING state"));
                    }
                } else {
                    logger.error("Unknown bike type: {}", bike.getType());
                    return CompletableFuture.failedFuture(new RuntimeException("Unknown bike type"));
                }

                if (user.getCredit() == 0) {
                    logger.error("User has no credit: {}", userId);
                    return CompletableFuture.failedFuture(new RuntimeException("User has no credit"));
                }

                if (bike.getBatteryLevel() == 0) {
                    logger.error("Bike has no battery: {}", bikeId);
                    return CompletableFuture.failedFuture(new RuntimeException("Bike has no battery"));
                }

                Ride ride = new Ride("ride-" + userId + "-" + bikeId, user, bike);
                rideRepository.addRide(ride);
                logger.info("Ride created: {}", ride.getId());

                rideEventsProducer.publishUpdate(event);

                rideRepository.getRideSimulation(ride.getId()).startSimulation().whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        logger.info("Ride simulation completed successfully: {}", ride.getId());
                        rideEventsProducer.publishUpdate(new RideStopEvent(userId, bikeId, ride.getBike().getType()));
                        rideRepository.removeRide(ride);
                    } else {
                        logger.error("Error during ride simulation: {}", throwable.getMessage());
                    }
                });

                return CompletableFuture.completedFuture(null);
            });
    }

    @Override
    public CompletableFuture<Void> stopRide(Event event) {
        if (!(event instanceof RequestRideEndEvent rideEndEvent)) {
            logger.error("Invalid event type for stopRide");
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid event type"));
        }
        String userId = rideEndEvent.username();
        logger.info("Stopping ride for user: {}", userId);
        return CompletableFuture.supplyAsync(() -> rideRepository.getRideSimulationByUserId(userId))
                .thenCompose(rideSimulation -> {
                    if (rideSimulation != null) {
                        String bikeId = rideSimulation.getRide().getBike().getId();
                        logger.info("Found active ride for user: {}, bike: {}", userId, bikeId);
                        rideSimulation.stopSimulationManually();
                        rideEventsProducer.publishUpdate(new RideStopEvent(userId, bikeId, rideSimulation.getRide().getBike().getType()));
                        return CompletableFuture.completedFuture(null);
                    }
                    logger.error("No active ride found for user: {}", userId);
                    return CompletableFuture.failedFuture(new RuntimeException("No active ride found"));
                });
    }


}