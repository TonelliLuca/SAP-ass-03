package application;

import application.ports.*;
import domain.model.*;
import infrastructure.repository.LocalProjectionRepository;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class RestRideServiceAPIImpl implements RestRideServiceAPI {
    private static final Logger logger = LoggerFactory.getLogger(RestRideServiceAPIImpl.class);

    private final RideRepository rideRepository;
    private final Vertx vertx;
    private final LocalProjectionRepository projectionRepository;
    private final RideEventsProducerPort rideEventsProducer;

    public RestRideServiceAPIImpl(
            EventPublisher publisher,
            Vertx vertx,
            LocalProjectionRepository projectionRepository,
            RideEventsProducerPort rideEventsProducer) {

        this.rideRepository = new RideRepositoryImpl(vertx, publisher);
        this.vertx = vertx;
        this.projectionRepository = projectionRepository;
        this.rideEventsProducer = rideEventsProducer;

        logger.info("RestRideServiceAPIImpl initialized");
    }

    @Override
    public CompletableFuture<Void> startRide(String userId, String bikeId) {
        logger.info("Starting ride for user: {} and bike: {}", userId, bikeId);

        // Get data from local projections using futures
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

                // Publish ride start event
                rideEventsProducer.publishRideStart(bikeId, userId);

                // Start simulation
                rideRepository.getRideSimulation(ride.getId()).startSimulation().whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        logger.info("Ride simulation completed successfully: {}", ride.getId());
                        rideEventsProducer.publishRideEnd(bikeId, userId);
                        rideRepository.removeRide(ride);
                    } else {
                        logger.error("Error during ride simulation: {}", throwable.getMessage());
                    }
                });

                return CompletableFuture.completedFuture(null);
            });
    }

    @Override
    public CompletableFuture<Void> stopRide(String userId) {
        logger.info("Stopping ride for user: {}", userId);
        return CompletableFuture.supplyAsync(() -> rideRepository.getRideSimulationByUserId(userId))
                .thenCompose(rideSimulation -> {
                    if (rideSimulation != null) {
                        String bikeId = rideSimulation.getRide().getEbike().getId();
                        logger.info("Found active ride for user: {}, bike: {}", userId, bikeId);
                        rideSimulation.stopSimulationManually();
                        rideEventsProducer.publishRideEnd(bikeId, userId);
                        return CompletableFuture.completedFuture(null);
                    }
                    logger.error("No active ride found for user: {}", userId);
                    return CompletableFuture.failedFuture(new RuntimeException("No active ride found"));
                });
    }
}