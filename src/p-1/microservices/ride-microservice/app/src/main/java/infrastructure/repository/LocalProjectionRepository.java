package infrastructure.repository;

import application.ports.ProjectionRepositoryPort;
import domain.model.EBike;
import domain.model.EBikeState;
import domain.model.User;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LocalProjectionRepository implements ProjectionRepositoryPort {
    private static final Logger logger = LoggerFactory.getLogger(LocalProjectionRepository.class);

    // Store projections
    private final Map<String, JsonObject> ebikeProjections = new ConcurrentHashMap<>();
    private final Map<String, JsonObject> userProjections = new ConcurrentHashMap<>();

    // Methods for E-bikes
    public CompletableFuture<Void> updateEBike(JsonObject ebikeJson) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Updating EBike projections");
            if (ebikeJson == null || !ebikeJson.containsKey("id")) {
                logger.error("EBike projection update failed");
                return;
            }
            String id = ebikeJson.getString("id");
            ebikeProjections.put(id, ebikeJson);
            logger.info("Updated e-bike projection: {}", id);
            logger.info("All EBike projections: {}", ebikeProjections);
        });
    }


    private JsonObject unwrapMap(JsonObject obj) {
        while (obj != null && obj.containsKey("map")) {
            obj = obj.getJsonObject("map");
        }
        return obj;
    }

    public CompletableFuture<EBike> getEBike(String id) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Getting e-bike projection: {}", id);
            JsonObject json = ebikeProjections.get(id);
            if (json == null) {
                logger.error("Getting e-bike projection failed");
                return null;
            }

            try {
                JsonObject location = unwrapMap(json.getJsonObject("location"));
                double x = location != null ? location.getDouble("x", 0.0) : 0.0;
                double y = location != null ? location.getDouble("y", 0.0) : 0.0;
                String stateStr = json.getString("state", "AVAILABLE");
                int batteryLevel = json.getInteger("batteryLevel", 100);

                return new EBike(id, x, y, EBikeState.valueOf(stateStr), batteryLevel);
            } catch (Exception e) {
                logger.error("Failed to convert e-bike JSON to model: {}", e.getMessage());
                logger.error(json.encodePrettily());
                return null;
            }
        });
    }

    // Methods for Users
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
            logger.info("All user projecitons: {}", userProjections);
        });
    }



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