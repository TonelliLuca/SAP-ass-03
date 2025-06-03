package infrastructure.repository;

import application.ports.ProjectionRepositoryPort;
import domain.model.*;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LocalProjectionRepository implements ProjectionRepositoryPort {
    private static final Logger logger = LoggerFactory.getLogger(LocalProjectionRepository.class);

    private final Map<String, JsonObject> bikeProjections = new ConcurrentHashMap<>();
    private final Map<String, JsonObject> userProjections = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> updateBike(JsonObject bikeJson) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Updating bike projection");
            if (bikeJson == null || !bikeJson.containsKey("id")) {
                logger.error("Bike projection update failed");
                return;
            }
            String id = bikeJson.getString("id");
            bikeProjections.put(id, bikeJson);
            logger.info("Updated bike projection: {}", id);
        });
    }

    private JsonObject unwrapMap(JsonObject obj) {
        while (obj != null && obj.containsKey("map")) {
            obj = obj.getJsonObject("map");
        }
        return obj;
    }

    @Override
    public CompletableFuture<Bike> getBike(String id) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Getting bike projection: {}", id);
            JsonObject json = bikeProjections.get(id);
            if (json == null) {
                logger.error("Getting bike projection failed");
                return null;
            }
            try {
                String type = json.getString("type", "ebike");
                JsonObject location = unwrapMap(json.getJsonObject("location", json.getJsonObject("position")));
                double x = location != null ? location.getDouble("x", 0.0) : 0.0;
                double y = location != null ? location.getDouble("y", 0.0) : 0.0;
                String stateStr = json.getString("state", "AVAILABLE");
                int batteryLevel = json.getInteger("batteryLevel", 100);

                if ("abike".equalsIgnoreCase(type)) {
                    return new ABike(id, x, y, BikeState.valueOf(stateStr), batteryLevel);
                } else {
                    return new EBike(id, x, y, BikeState.valueOf(stateStr), batteryLevel);
                }
            } catch (Exception e) {
                logger.error("Failed to convert bike JSON to model: {}", e.getMessage());
                logger.error(json.encodePrettily());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> updateUser(JsonObject userJson) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Updating user projection: {}", userJson.encodePrettily());
            if (!userJson.containsKey("username")) {
                logger.error("User projection update failed");
                return;
            }
            String username = userJson.getString("username");
            userProjections.put(username, userJson);
            logger.info("Updated user projection: {}", username);
        });
    }

    @Override
    public CompletableFuture<User> getUser(String username) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Getting user projection: {}", username);
            JsonObject json = userProjections.get(username);
            if (json == null) {
                logger.error("Getting user projection failed");
                return null;
            }
            try {
                int credit = json.getInteger("credit", 0);
                return new User(username, credit);
            } catch (Exception e) {
                logger.error("Failed to convert user JSON to model: {}", e.getMessage());
                return null;
            }
        });
    }
}