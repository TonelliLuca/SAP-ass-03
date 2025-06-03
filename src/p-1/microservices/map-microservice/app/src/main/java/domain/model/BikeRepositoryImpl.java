package domain.model;

import ddd.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BikeRepositoryImpl implements BikeRepository, Repository {
    private final ConcurrentHashMap<String, Bike> bikes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> bikeAssignments = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> saveBike(Bike bike) {
        bikes.put(bike.getId(), bike);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Bike> getBike(String bikeName) {
        Bike bike = bikes.get(bikeName);
        if (bike != null) {
            return CompletableFuture.completedFuture(bike);
        } else {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Bike not found"));
        }
    }

    public CompletableFuture<Map<String, List<Bike>>> getUsersWithAssignedAndAvailableBikes() {
        return CompletableFuture.supplyAsync(() -> {
            List<Bike> availableBikes = bikes.values().stream()
                    .filter(bike -> bike.getState() == BikeState.AVAILABLE)
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
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(bikes.values()));
    }

    @Override
    public CompletableFuture<List<Bike>> getAllBikes(String username) {
        return CompletableFuture.supplyAsync(() -> bikes.values().stream()
                .filter(bike -> {
                    String assignedBikeName = bikeAssignments.get(username);
                    return assignedBikeName != null && assignedBikeName.equals(bike.getId());
                })
                .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Void> assignBikeToUser(String username, Bike bike) {
        return CompletableFuture.runAsync(() -> {
            if (!bikes.containsKey(bike.getId())) {
                throw new IllegalArgumentException("Bike not found in repository");
            }

            if (bikeAssignments.containsValue(bike.getId())) {
                throw new IllegalStateException("Bike is already assigned to another user");
            }

            bikeAssignments.put(username, bike.getId());
        });
    }

    @Override
    public CompletableFuture<Void> unassignBikeFromUser(String username, Bike bike) {
        return CompletableFuture.runAsync(() -> {
            if (!bikeAssignments.containsKey(username)) {
                throw new IllegalArgumentException("User does not have any bike assigned");
            }

            if (!bikeAssignments.get(username).equals(bike.getId())) {
                throw new IllegalArgumentException("Bike is not assigned to the user");
            }

            bikeAssignments.remove(username);
        });
    }

    @Override
    public CompletableFuture<List<Bike>> getAvailableBikes() {
        return CompletableFuture.supplyAsync(() -> bikes.values().stream()
                .filter(bike -> bike.getState() == BikeState.AVAILABLE)
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
}