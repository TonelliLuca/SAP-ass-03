package application.ports;

import domain.event.ABikeUpdateEvent;
import domain.event.EBikeUpdateEvent;
import domain.event.UserUpdateEvent;
import domain.model.Bike;
import domain.model.User;

import java.util.concurrent.CompletableFuture;

public interface ProjectionRepositoryPort {
    CompletableFuture<Void> appendUserEvent(UserUpdateEvent event);
    CompletableFuture<Void> appendEBikeEvent(EBikeUpdateEvent event);
    CompletableFuture<Void> appendABikeEvent(ABikeUpdateEvent event);
    CompletableFuture<Bike> getBike(String id, String bikeType);
    CompletableFuture<User> getUser(String username);
}