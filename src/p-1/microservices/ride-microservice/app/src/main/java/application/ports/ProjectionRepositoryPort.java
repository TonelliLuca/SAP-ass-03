package application.ports;

import domain.model.Bike;
import domain.model.User;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public interface ProjectionRepositoryPort {
    CompletableFuture<Bike> getBike(String id);
    CompletableFuture<Void> updateBike(JsonObject ebikeJson);
    CompletableFuture<Void> updateUser(JsonObject userJson);
    CompletableFuture<User> getUser(String username) ;
}
