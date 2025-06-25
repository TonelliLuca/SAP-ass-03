package infrastructure.repository;

import application.ports.ProjectionRepositoryPort;
import domain.event.EBikeUpdateEvent;
import domain.event.UserUpdateEvent;
import domain.model.EBike;
import domain.model.EBikeState;
import domain.model.P2d;
import domain.model.User;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LocalProjectionRepository implements ProjectionRepositoryPort {
    private static final Logger logger = LoggerFactory.getLogger(LocalProjectionRepository.class);

    // Mappa <username, lista eventi>
    private final Map<String, List<UserUpdateEvent>> userEvents = new ConcurrentHashMap<>();
    private final Map<String, List<EBikeUpdateEvent>> ebikeEvents = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> appendUserEvent(UserUpdateEvent event) {
        userEvents.computeIfAbsent(event.username(), k -> new ArrayList<>()).add(event);
        logger.info("Appended UserUpdateEvent for {}", event.username());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> appendEBikeEvent(EBikeUpdateEvent event) {
        ebikeEvents.computeIfAbsent(event.bikeId(), k -> new ArrayList<>()).add(event);
        logger.info("Appended EBikeUpdateEvent for {}", event.bikeId());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<User> getUser(String username) {
        return CompletableFuture.supplyAsync(() -> {
            List<UserUpdateEvent> events = userEvents.get(username);
            if (events == null || events.isEmpty()) return null;
            UserUpdateEvent latest = events.stream()
                    .max(Comparator.comparing(UserUpdateEvent::timestamp))
                    .orElse(null);
            if (latest == null) return null;
            return new User(latest.username(), latest.credit());
        });
    }

    @Override
    public CompletableFuture<EBike> getEBike(String bikeId) {
        return CompletableFuture.supplyAsync(() -> {
            List<EBikeUpdateEvent> events = ebikeEvents.get(bikeId);
            if (events == null || events.isEmpty()) return null;
            EBikeUpdateEvent latest = events.stream()
                    .max(Comparator.comparing(EBikeUpdateEvent::timestamp))
                    .orElse(null);
            if (latest == null) return null;
            return new EBike(
                    latest.bikeId(),
                    latest.location().x(),
                    latest.location().y(),
                    latest.state(),
                    latest.batteryLevel()
            );
        });
    }

}
