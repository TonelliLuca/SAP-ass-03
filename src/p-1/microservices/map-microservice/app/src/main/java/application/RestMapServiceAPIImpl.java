package application;

import application.ports.RestMapServiceAPI;

import domain.model.*;
import application.ports.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class RestMapServiceAPIImpl implements RestMapServiceAPI {
    private final StationRepository stationRepository;
    private final BikeRepository bikeRepository;
    private final EventPublisher eventPublisher;
    private final List<String> registeredUsers = new CopyOnWriteArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(RestMapServiceAPIImpl.class);

    public RestMapServiceAPIImpl(EventPublisher eventPublisher) {
        this.bikeRepository = new BikeRepositoryImpl();
        this.stationRepository = new StationRepositoryImpl();
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CompletableFuture<Void> updateBikes(List<Bike> bikes) {
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
    public CompletableFuture<Void> updateBike(Bike bike) {
        logger.info("Updating Bike: {}", bike.getId());
        return bikeRepository.saveBike(bike)
                .thenAccept(v -> {
                    logger.info("Saved Bike: {}", bike.getId());
                    bikeRepository.getAllBikes().thenAccept(bikes -> {
                        bikeRepository.getAllMovingAbikes().thenAccept(movingAbikes -> {
                            bikes.addAll(movingAbikes);
                            logger.info("Publishing all ebikes + movingAbikes: {}", bikes);
                            eventPublisher.publishBikesUpdate(bikes);
                        });
                    });

                    bikeRepository.getUsersWithAssignedAndAvailableBikes().thenAccept(usersWithBikeMap -> {
                        logger.info("Users with assigned/available bikes: {}", usersWithBikeMap.keySet());
                        if(!usersWithBikeMap.isEmpty()){
                            usersWithBikeMap.forEach((username, userBikes) -> {
                                bikeRepository.getAssignedABike(username).thenAccept(assignedABikes -> {
                                    assignedABikes.stream()
                                            .filter(abike -> userBikes.stream().noneMatch(b -> b.getId().equals(abike.getId())))
                                            .forEach(userBikes::add);
                                    logger.info("Publishing all assigned ebikes + assigned ABikes: {} for user {}", userBikes, username);
                                    eventPublisher.publishUserBikesUpdate(userBikes, username);
                                });
                            });

                            registeredUsers.stream()
                                    .filter(user -> !usersWithBikeMap.containsKey(user))
                                    .forEach(user -> bikeRepository.getAvailableBikes().thenAccept(availableBikes -> {
                                        logger.info("Publishing available bikes to unassigned user: {}", user);
                                        eventPublisher.publishUserBikesUpdate(availableBikes, user);
                                    }));
                        }
                        else{
                            bikeRepository.getAvailableBikes().thenAccept(availableBikes -> {
                                logger.info("Publishing available bikes update, count: {}", availableBikes.size());
                                eventPublisher.publishUserAvailableBikesUpdate(availableBikes);
                            });
                        }
                    });


                });
    }

    @Override
    public CompletableFuture<Void> notifyStartRide(String username, String bikeName, String bikeType) {
        logger.info("notifyStartRide: username={}, bikeName={} bikeType={}", username, bikeName, bikeType);
        return bikeRepository.getBike(bikeName, bikeType)
                 .thenComposeAsync(bike -> bikeRepository.assignBikeToUser(username, bike))
                 .thenAcceptAsync(v -> bikeRepository.getAvailableBikes().thenAcceptAsync(eventPublisher::publishUserAvailableBikesUpdate));
    }


    @Override
    public CompletableFuture<Void> notifyStopRide(String username, String bikeName, String bikeType) {
        return bikeRepository.getBike(bikeName, bikeType)
                .thenCompose(bike -> bikeRepository.unassignBikeFromUser(username, bike))
                .thenAccept(v -> {
                    bikeRepository.getAvailableBikes().thenAccept(eventPublisher::publishUserAvailableBikesUpdate);
                    eventPublisher.publishStopRide(username);
                });
    }

    @Override
    public void getAllBikes() {
        bikeRepository.getAllBikes().thenAccept(bikes -> {
            bikes.removeIf(bike -> bike instanceof ABike && bike.getState() == BikeState.AVAILABLE);
            eventPublisher.publishBikesUpdate(bikes);
        });
    }

    @Override
    public void getAllBikes(String username) {
        List<Bike> availableBikes = bikeRepository.getAvailableBikes().join();
        List<Bike> userBikes = bikeRepository.getAllBikes(username).join();
        availableBikes.removeIf(bike -> bike instanceof ABike && bike.getState() == BikeState.AVAILABLE);
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

    @Override
    public void updateStation(Station station) {
        stationRepository.saveStation(station)
            .thenCompose(v -> stationRepository.getAllStations())
            .thenAccept(eventPublisher::publishStationsUpdate)
            .exceptionally(ex -> {
                logger.error("Failed to update station: {}", ex.getMessage());
                return null;
            });
    }

    @Override
    public void getAllStations() {
        stationRepository.getAllStations()
            .thenAccept(eventPublisher::publishStationsUpdate)
            .exceptionally(ex -> {
                logger.error("Failed to get all stations: {}", ex.getMessage());
                return null;
            });
    }

    @Override
    public void notifyABikeArrivedToUser(String userId, String abikeId) {
        eventPublisher.publishABikeArrivedToUser(userId, abikeId);
    }

}
