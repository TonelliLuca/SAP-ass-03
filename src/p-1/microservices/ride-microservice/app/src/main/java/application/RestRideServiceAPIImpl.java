package application;

import application.ports.*;
import domain.event.*;
import domain.model.*;
import infrastructure.repository.RideRepositoryImpl;
import io.vertx.core.Vertx;
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
        if (!(event instanceof RideStartEvent rideStartEvent)) {
            logger.error("Event is not a RideStartEvent: {}", event);
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid event type"));
        }
        String userId = rideStartEvent.username();
        String bikeId = rideStartEvent.bikeId();
        logger.info("Event-based startRide received for user: {} bike: {}", userId, bikeId);

        CompletableFuture<EBike> ebikeFuture = projectionRepository.getEBike(bikeId);
        CompletableFuture<User> userFuture = projectionRepository.getUser(userId);

        return CompletableFuture.allOf(ebikeFuture, userFuture)
                .thenCompose(v -> {
                    EBike ebike = ebikeFuture.join();
                    User user = userFuture.join();

                    if (ebike == null) {
                        logger.error("EBike not found in projection: {}", bikeId);
                        return CompletableFuture.failedFuture(new RuntimeException("EBike not found"));
                    }
                    if (user == null) {
                        logger.error("User not found in projection: {}", userId);
                        return CompletableFuture.failedFuture(new RuntimeException("User not found"));
                    }
                    if (ebike.getState() != EBikeState.AVAILABLE) {
                        logger.error("EBike is not available: {}, state: {}", bikeId, ebike.getState());
                        return CompletableFuture.failedFuture(new RuntimeException("EBike is not available"));
                    }
                    if (user.getCredit() == 0) {
                        logger.error("User has no credit: {}", userId);
                        return CompletableFuture.failedFuture(new RuntimeException("User has no credit"));
                    }
                    if (ebike.getBatteryLevel() == 0) {
                        logger.error("EBike has no battery: {}", bikeId);
                        return CompletableFuture.failedFuture(new RuntimeException("EBike has no battery"));
                    }

                    // Create ride
                    Ride ride = new Ride("ride-" + userId + "-" + bikeId, user, ebike);
                    rideRepository.addRide(ride);
                    logger.info("Ride created: {}", ride.getId());

                    rideEventsProducer.publishUpdate(rideStartEvent);
                    logger.info("Published RideStartEvent: {}", rideStartEvent);

                    rideRepository.getRideSimulation(ride.getId()).startSimulation().whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            logger.info("Ride simulation completed successfully: {}", ride.getId());
                            RideStopEvent stopEvent = new RideStopEvent(userId, bikeId);
                            rideEventsProducer.publishUpdate(stopEvent);
                            rideRepository.removeRide(ride);
                            logger.info("Published RideStopEvent: {}", stopEvent);
                        } else {
                            logger.error("Error during ride simulation: {}", throwable.getMessage());
                        }
                    });

                    return CompletableFuture.completedFuture(null);
                });
    }

    @Override
    public CompletableFuture<Void> stopRide(Event event) {
        if (!(event instanceof RequestRideEndEvent requestEndEvent)) {
            logger.error("Event is not a RequestRideEndEvent: {}", event);
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid event type"));
        }
        String userId = requestEndEvent.username();
        logger.info("Event-based stopRide received for user: {}", userId);

        return CompletableFuture.supplyAsync(() -> rideRepository.getRideSimulationByUserId(userId))
                .thenCompose(rideSimulation -> {
                    if (rideSimulation != null) {
                        String bikeId = rideSimulation.getRide().getEbike().getId();
                        logger.info("Found active ride for user: {}, bike: {}", userId, bikeId);
                        rideSimulation.stopSimulationManually();

                        RideStopEvent stopEvent = new RideStopEvent(userId, bikeId);
                        rideEventsProducer.publishUpdate(stopEvent);
                        logger.info("Published RideStopEvent: {}", stopEvent);

                        rideRepository.removeRide(rideSimulation.getRide());
                        return CompletableFuture.completedFuture(null);
                    }
                    logger.error("No active ride found for user: {}", userId);
                    return CompletableFuture.failedFuture(new RuntimeException("No active ride found"));
                });
    }

}
