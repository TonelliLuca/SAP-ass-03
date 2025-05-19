package application;

import application.ports.RestMapServiceAPI;

import domain.model.EBike;
import application.ports.EventPublisher;
import domain.model.EBikeRepository;
import domain.model.EBikeRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class RestMapServiceAPIImpl implements RestMapServiceAPI {

    private final EBikeRepository bikeRepository;
    private final EventPublisher eventPublisher;
    private final List<String> registeredUsers = new CopyOnWriteArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(RestMapServiceAPIImpl.class);

    public RestMapServiceAPIImpl(EventPublisher eventPublisher) {
        this.bikeRepository = new EBikeRepositoryImpl();
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CompletableFuture<Void> updateEBikes(List<EBike> bikes) {
        return CompletableFuture.allOf(bikes.stream()
                .map(bikeRepository::saveBike)
                .toArray(CompletableFuture[]::new))
                .thenAccept(v -> {
                    bikeRepository.getAllBikes().thenAccept(eventPublisher::publishBikesUpdate);

                    bikeRepository.getUsersWithAssignedAndAvailableBikes().thenAccept(usersWithBikeMap -> {
                        if(!usersWithBikeMap.isEmpty()){
                            usersWithBikeMap.forEach((username, userBikes) -> eventPublisher.publishUserBikesUpdate(userBikes, username));
                        }
                        else{
                            bikeRepository.getAvailableBikes().thenAccept(eventPublisher::publishUserAvailableBikesUpdate);
                        }
                    });

                });
    }

    @Override
    public CompletableFuture<Void> updateEBike(EBike bike) {
        logger.debug("Updating eBike: {}", bike.getId());
        return bikeRepository.saveBike(bike)
                .thenAccept(v -> {
                    logger.debug("Saved eBike: {}", bike.getId());
                    bikeRepository.getAllBikes().thenAccept(bikes -> {
                        logger.debug("Publishing all bikes update, count: {}", bikes.size());
                        eventPublisher.publishBikesUpdate(bikes);
                    });

                    bikeRepository.getUsersWithAssignedAndAvailableBikes().thenAccept(usersWithBikeMap -> {
                        logger.debug("Users with assigned/available bikes: {}", usersWithBikeMap.keySet());
                        if(!usersWithBikeMap.isEmpty()){
                            usersWithBikeMap.forEach((username, userBikes) -> {
                                logger.debug("Publishing user bikes update for user: {}", username);
                                eventPublisher.publishUserBikesUpdate(userBikes, username);
                            });

                            registeredUsers.stream()
                                    .filter(user -> !usersWithBikeMap.containsKey(user))
                                    .forEach(user -> bikeRepository.getAvailableBikes().thenAccept(availableBikes -> {
                                        logger.debug("Publishing available bikes to unassigned user: {}", user);
                                        eventPublisher.publishUserBikesUpdate(availableBikes, user);
                                    }));
                        }
                        else{
                            bikeRepository.getAvailableBikes().thenAccept(availableBikes -> {
                                logger.debug("Publishing available bikes update, count: {}", availableBikes.size());
                                eventPublisher.publishUserAvailableBikesUpdate(availableBikes);
                            });
                        }
                    });

                });
    }

    @Override
    public CompletableFuture<Void> notifyStartRide(String username, String bikeName) {
         return bikeRepository.getBike(bikeName)
                 .thenCompose(bike -> bikeRepository.assignBikeToUser(username, bike))
                 .thenAccept(v -> bikeRepository.getAvailableBikes().thenAccept(eventPublisher::publishUserAvailableBikesUpdate));
    }


    @Override
    public CompletableFuture<Void> notifyStopRide(String username, String bikeName) {
        return bikeRepository.getBike(bikeName)
                .thenCompose(bike -> bikeRepository.unassignBikeFromUser(username, bike))
                .thenAccept(v -> {
                    bikeRepository.getAvailableBikes().thenAccept(eventPublisher::publishUserAvailableBikesUpdate);
                    eventPublisher.publishStopRide(username);
                });
    }

    @Override
    public void getAllBikes() {
        bikeRepository.getAllBikes().thenAccept(eventPublisher::publishBikesUpdate);
    }

    @Override
    public void getAllBikes(String username) {
        List<EBike> availableBikes = bikeRepository.getAvailableBikes().join();
        List<EBike> userBikes = bikeRepository.getAllBikes(username).join();
        if(!userBikes.isEmpty()){
            availableBikes.addAll(userBikes);
            eventPublisher.publishUserBikesUpdate(availableBikes, username);
        }
        else{
            System.out.println("No bikes assigned to user: " + username);
            System.out.println("Available bikes: " + availableBikes);
            eventPublisher.publishUserAvailableBikesUpdate(availableBikes);
        }

    }

    @Override
    public void registerUser(String username) {
        registeredUsers.add(username);
    }

    @Override
    public void deregisterUser(String username) {
        registeredUsers.remove(username);
    }

}
