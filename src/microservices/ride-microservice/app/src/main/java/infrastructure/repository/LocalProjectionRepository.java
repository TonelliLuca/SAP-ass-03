package infrastructure.repository;

import domain.model.EBike;
import domain.model.EBikeState;
import domain.model.User;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LocalProjectionRepository {
    private static final Logger logger = LoggerFactory.getLogger(LocalProjectionRepository.class);

    // Store projections
    private final Map<String, JsonObject> ebikeProjections = new ConcurrentHashMap<>();
    private final Map<String, JsonObject> userProjections = new ConcurrentHashMap<>();

    // Methods for E-bikes
    public CompletableFuture<Void> updateEBike(JsonObject ebikeJson) {
        return CompletableFuture.runAsync(() -> {
            if (ebikeJson == null || !ebikeJson.containsKey("id")) {
                return;
            }
            String id = ebikeJson.getString("id");
            ebikeProjections.put(id, ebikeJson);
            logger.info("Updated e-bike projection: {}", id);
        });
    }

    public CompletableFuture<JsonObject> getEBikeJson(String id) {
        return CompletableFuture.supplyAsync(() -> ebikeProjections.get(id));
    }

    public CompletableFuture<EBike> getEBike(String id) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject json = ebikeProjections.get(id);
            if (json == null) {
                return null;
            }

            try {
                JsonObject location = json.getJsonObject("location");
                double x = location != null ? location.getDouble("x") : 0;
                double y = location != null ? location.getDouble("y") : 0;
                String stateStr = json.getString("state", "AVAILABLE");
                int batteryLevel = json.getInteger("batteryLevel", 100);

                return new EBike(id, x, y, EBikeState.valueOf(stateStr), batteryLevel);
            } catch (Exception e) {
                logger.error("Failed to convert e-bike JSON to model: {}", e.getMessage());
                return null;
            }
        });
    }

    // Methods for Users
    public CompletableFuture<Void> updateUser(JsonObject userJson) {
        return CompletableFuture.runAsync(() -> {
            if (userJson == null || !userJson.containsKey("username")) {
                return;
            }
            String username = userJson.getString("username");
            userProjections.put(username, userJson);
            logger.info("Updated user projection: {}", username);
        });
    }

    public CompletableFuture<JsonObject> getUserJson(String username) {
        return CompletableFuture.supplyAsync(() -> userProjections.get(username));
    }

    public CompletableFuture<User> getUser(String username) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject json = userProjections.get(username);
            if (json == null) {
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

    public CompletableFuture<Void> clear() {
        return CompletableFuture.runAsync(() -> {
            ebikeProjections.clear();
            userProjections.clear();
        });
    }
}