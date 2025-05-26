package application.ports;

import domain.model.EBike;
import domain.model.User;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public interface ProjectionRepositoryPort {
    CompletableFuture<EBike> getEBike(String id);
    CompletableFuture<Void> updateEBike(JsonObject ebikeJson);
    CompletableFuture<Void> updateUser(JsonObject userJson);
    CompletableFuture<User> getUser(String username) ;


}
