package infrastructure.persistence;

import application.ports.EBikeRepository;
import domain.model.EBike;
import domain.model.EBikeState;
import domain.model.P2d;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MongoEBikeRepository implements EBikeRepository {
    private final MongoClient mongoClient;
    private static final String COLLECTION = "ebikes";

    public MongoEBikeRepository(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public CompletableFuture<Void> save(EBike ebike) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (ebike == null || ebike.getId() == null) {
            future.completeExceptionally(new IllegalArgumentException("Invalid ebike data"));
            return future;
        }

        JsonObject document = new JsonObject()
                .put("_id", ebike.getId())
                .put("state", ebike.getState().name())
                .put("batteryLevel", ebike.getBatteryLevel())
                .put("location", p2dToJson(ebike.getLocation()));

        mongoClient.insert(COLLECTION, document)
                .onSuccess(result -> future.complete(null))
                .onFailure(error -> future.completeExceptionally(
                        new RuntimeException("Failed to save ebike: " + error.getMessage())));
        return future;
    }

    @Override
    public CompletableFuture<Void> update(EBike ebike) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (ebike == null || ebike.getId() == null) {
            future.completeExceptionally(new IllegalArgumentException("Invalid ebike data"));
            return future;
        }
        JsonObject query = new JsonObject().put("_id", ebike.getId());

        JsonObject updateDoc = new JsonObject()
                .put("state", ebike.getState().name())
                .put("batteryLevel", ebike.getBatteryLevel())
                .put("location", p2dToJson(ebike.getLocation()));

        JsonObject update = new JsonObject().put("$set", updateDoc);

        mongoClient.findOneAndUpdate(COLLECTION, query, update)
                .onSuccess(result -> {
                    if (result != null) {
                        future.complete(null);
                    } else {
                        future.completeExceptionally(new RuntimeException("EBike not found"));
                    }
                })
                .onFailure(error -> future.completeExceptionally(
                        new RuntimeException("Failed to update ebike: " + error.getMessage())));
        return future;
    }

    @Override
    public CompletableFuture<Optional<EBike>> findById(String id) {
        CompletableFuture<Optional<EBike>> future = new CompletableFuture<>();
        if (id == null || id.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("Invalid id"));
            return future;
        }
        JsonObject query = new JsonObject().put("_id", id);

        mongoClient.findOne(COLLECTION, query, null)
                .onSuccess(result -> {
                    if (result != null) {
                        future.complete(Optional.of(jsonToEbike(result)));
                    } else {
                        future.complete(Optional.empty());
                    }
                })
                .onFailure(error -> future.completeExceptionally(
                        new RuntimeException("Failed to find ebike: " + error.getMessage())));
        return future;
    }

    @Override
    public CompletableFuture<List<EBike>> findAll() {
        CompletableFuture<List<EBike>> future = new CompletableFuture<>();
        JsonObject query = new JsonObject();

        mongoClient.find(COLLECTION, query)
                .onSuccess(results -> {
                    List<EBike> ebikes = results.stream()
                            .map(this::jsonToEbike)
                            .toList();
                    future.complete(ebikes);
                })
                .onFailure(future::completeExceptionally);

        return future;
    }

    // ------ Helper Methods ------

    private JsonObject p2dToJson(P2d p) {
        return new JsonObject().put("x", p.getX()).put("y", p.getY());
    }

    private EBike jsonToEbike(JsonObject obj) {
        String id = obj.getString("_id");
        EBikeState state = EBikeState.valueOf(obj.getString("state"));
        int batteryLevel = obj.getInteger("batteryLevel");
        JsonObject loc = obj.getJsonObject("location");
        P2d location = new P2d(loc.getDouble("x"), loc.getDouble("y"));
        return new EBike(id, location, state, batteryLevel);
    }
}
