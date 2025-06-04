package infrastructure.repository.mongo;

import application.port.ABikeRepository;
import domain.model.ABike;
import domain.model.ABikeState;
import domain.model.P2d;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ABikeMongoRepository implements ABikeRepository {
    private final MongoClient mongoClient;
    private static final String COLLECTION = "abikes";
    private final Logger logger = LoggerFactory.getLogger(ABikeMongoRepository.class);

    public ABikeMongoRepository(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    private JsonObject abikeToDocument(ABike abike) {
        return new JsonObject()
                .put("_id", abike.id())
                .put("batteryLevel", abike.batteryLevel())
                .put("state", abike.state().name())
                .put("stationId", abike.stationId())
                .put("position", new JsonObject()
                        .put("x", abike.position().x())
                        .put("y", abike.position().y()));
    }

    private ABike documentToABike(JsonObject doc) {
        String id = doc.getString("_id");
        int batteryLevel = doc.getInteger("batteryLevel");
        ABikeState state = ABikeState.valueOf(doc.getString("state"));
        String stationId = doc.getString("stationId");
        JsonObject pos = doc.getJsonObject("position");
        P2d position = new P2d(pos.getDouble("x"), pos.getDouble("y"));
        return new ABike(id, position, batteryLevel, state, stationId);
    }

    @Override
    public CompletableFuture<Void> save(ABike abike) {
        logger.info("saving ABike {}", abike.id());
        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject doc = abikeToDocument(abike);

        mongoClient.save(COLLECTION, doc)
                .onSuccess(res -> future.complete(null))
                .onFailure(err -> future.completeExceptionally(
                        new RuntimeException("Failed to save abike: " + err.getMessage())));
        return future;
    }

    @Override
    public CompletableFuture<ABike> findById(String id) {
        CompletableFuture<ABike> future = new CompletableFuture<>();
        JsonObject query = new JsonObject().put("_id", id);

        mongoClient.findOne(COLLECTION, query, null)
                .onSuccess(doc -> {
                    if (doc != null) {
                        future.complete(documentToABike(doc));
                    } else {
                        future.complete(null);
                    }
                })
                .onFailure(err -> future.completeExceptionally(
                        new RuntimeException("Failed to find abike: " + err.getMessage())));
        return future;
    }

    @Override
    public CompletableFuture<HashSet<ABike>> findAll() {
        CompletableFuture<HashSet<ABike>> future = new CompletableFuture<>();
        mongoClient.find(COLLECTION, new JsonObject())
                .onSuccess(results -> {
                    HashSet<ABike> abikes = new HashSet<>();
                    for (JsonObject doc : results) {
                        abikes.add(documentToABike(doc));
                    }
                    future.complete(abikes);
                })
                .onFailure(future::completeExceptionally);
        return future;
    }
    @Override
    public CompletableFuture<Void> update(ABike abike) {
        logger.info("updating ABike {}", abike.id());
        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject query = new JsonObject().put("_id", abike.id());
        JsonObject update = new JsonObject().put("$set", abikeToDocument(abike));

        mongoClient.updateCollection(COLLECTION, query, update)
            .onSuccess(res -> future.complete(null))
            .onFailure(err -> future.completeExceptionally(
                new RuntimeException("Failed to update abike: " + err.getMessage())));
        return future;
    }
}