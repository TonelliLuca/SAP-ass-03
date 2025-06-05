package domain.model;

import ddd.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BikeRepositoryImpl implements BikeRepository, Repository {
    private final ConcurrentHashMap<String, Bike> bikes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> bikeAssignments = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(BikeRepositoryImpl.class);


    private String bikeKey(String id, String type) {
        return id + ":" + type.toLowerCase();
    }

    @Override
    public CompletableFuture<Void> saveBike(Bike bike) {
        bikes.put(bikeKey(bike.getId(), bike instanceof ABike ? "abike" : "ebike"), bike);
        logger.info("Saving bike " + bike);
        logger.info("Bikes "+ bikes.size());
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Bike> getBike(String id, String type) {

        Bike bike = bikes.get(bikeKey(id, type));
        logger.info("Getting bike " + bike);
        if (bike != null) {
            return CompletableFuture.completedFuture(bike);
        } else {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Bike not found"));
        }
    }

    public CompletableFuture<Map<String, List<Bike>>> getUsersWithAssignedAndAvailableBikes() {
        return CompletableFuture.supplyAsync(() -> {
            List<Bike> availableBikes = bikes.values().stream()
                    .filter(bike -> bike instanceof EBike && bike.getState() == BikeState.AVAILABLE)
                    .toList();

            return bikeAssignments.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> {
                                List<Bike> userBikes = bikes.values().stream()
                                        .filter(bike -> bike.getId().equals(entry.getValue()))
                                        .collect(Collectors.toList());

                                userBikes.addAll(availableBikes);
                                return userBikes;
                            }
                    ));
        });
    }


    @Override
    public CompletableFuture<List<Bike>> getAllBikes() {
        return CompletableFuture.supplyAsync(() -> bikes.values().stream()
            .filter(bike -> !(bike instanceof ABike && bike.getState() == BikeState.AVAILABLE))
            .collect(Collectors.toList()));
    }


    @Override
    public CompletableFuture<List<Bike>> getAllBikes(String username) {
        return CompletableFuture.supplyAsync(() -> bikes.values().stream()
            .filter(bike -> {
                String assignedBikeName = bikeAssignments.get(username);
                return assignedBikeName != null && assignedBikeName.equals(bike.getId());
            })
            .filter(bike -> !(bike instanceof ABike && bike.getState() == BikeState.AVAILABLE))
            .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Void> assignBikeToUser(String username, Bike bike) {
        logger.info("Assigning bike {}, to user {}", bike, username);
        return CompletableFuture.runAsync(() -> {
            String key = bikeKey(bike.getId(), bike instanceof ABike ? "abike" : "ebike");
            if (!bikes.containsKey(key)) {
                throw new IllegalArgumentException("Bike not found in repository");
            }
            if (bikeAssignments.containsValue(bike.getId())) {
                throw new IllegalStateException("Bike is already assigned to another user");
            }
            bikeAssignments.put(username, bike.getId());
            logger.info("bikeAssignments " + bikeAssignments);
        });
    }

    @Override
    public CompletableFuture<Void> unassignBikeFromUser(String username, Bike bike) {
        logger.info("Unassigning bike {} from user {}", bike, bike.getId());
        return CompletableFuture.runAsync(() -> {
            if (!bikeAssignments.containsKey(username)) {
                throw new IllegalArgumentException("User does not have any bike assigned");
            }

            if (!bikeAssignments.get(username).equals(bike.getId())) {
                throw new IllegalArgumentException("Bike is not assigned to the user");
            }

            bikeAssignments.remove(username);
            logger.info("bikeAssignments " + bikeAssignments);
        });
    }

    @Override
    public CompletableFuture<List<Bike>> getAvailableBikes() {
        return CompletableFuture.supplyAsync(() -> bikes.values().stream()
                .filter(bike -> bike instanceof EBike && bike.getState() == BikeState.AVAILABLE)
                .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<String> isBikeAssigned(Bike bike) {
        return CompletableFuture.supplyAsync(() -> {
            for (Map.Entry<String, String> entry : bikeAssignments.entrySet()) {
                if (entry.getValue().equals(bike.getId())) {
                    return entry.getKey();
                }
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<List<Bike>> getAssignedABike(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String assignedId = bikeAssignments.get(username);
            if (assignedId == null) return List.of();
            Bike bike = bikes.get(bikeKey(assignedId, "abike"));
            if (bike instanceof ABike) {
                return List.of(bike);
            }
            return List.of();
        });
    }

    @Override
    public CompletableFuture<List<Bike>> getAllMovingAbikes() {
        return CompletableFuture.supplyAsync(() -> bikes.values().stream()
            .filter(bike -> bike instanceof ABike && bike.getState() != BikeState.AVAILABLE)
            .toList());
    }
}