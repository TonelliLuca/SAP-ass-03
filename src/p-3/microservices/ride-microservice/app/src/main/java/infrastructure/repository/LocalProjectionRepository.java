package infrastructure.repository;

import application.ports.ProjectionRepositoryPort;
import domain.event.EBikeUpdateEvent;
import domain.event.ABikeUpdateEvent;
import domain.event.UserUpdateEvent;
import domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LocalProjectionRepository implements ProjectionRepositoryPort {
    private static final Logger logger = LoggerFactory.getLogger(LocalProjectionRepository.class);

    // Map <username, list of user events>
    private final Map<String, List<UserUpdateEvent>> userEvents = new ConcurrentHashMap<>();
    // Map <id:type, list of bike events>
    private final Map<String, List<Object>> bikeEvents = new ConcurrentHashMap<>();

    private String bikeKey(String id, String type) {
        return id + ":" + type.toLowerCase();
    }

    @Override
    public CompletableFuture<Void> appendUserEvent(UserUpdateEvent event) {
        userEvents.computeIfAbsent(event.username(), k -> new ArrayList<>()).add(event);
        logger.info("Appended UserUpdateEvent for {}", event.username());
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> appendEBikeEvent(EBikeUpdateEvent event) {
        bikeEvents.computeIfAbsent(bikeKey(event.bikeId(), "ebike"), k -> new ArrayList<>()).add(event);
        logger.info("Appended EBikeUpdateEvent for {}", event.bikeId());
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> appendABikeEvent(ABikeUpdateEvent event) {
        bikeEvents.computeIfAbsent(bikeKey(event.bikeId(), "abike"), k -> new ArrayList<>()).add(event);
        logger.info("Appended ABikeUpdateEvent for {}", event.bikeId());
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
    public CompletableFuture<Bike> getBike(String bikeId, String type) {
        return CompletableFuture.supplyAsync(() -> {
            List<Object> events = bikeEvents.get(bikeKey(bikeId, type));
            if (events == null || events.isEmpty()) return null;
            Object latest = events.stream()
                    .max(Comparator.comparing(e -> {
                        if (e instanceof EBikeUpdateEvent) {
                            return ((EBikeUpdateEvent) e).timestamp();
                        } else if (e instanceof ABikeUpdateEvent) {
                            return ((ABikeUpdateEvent) e).timestamp();
                        }
                        return "0";
                    }))
                    .orElse(null);
            if (latest == null) return null;
            if ("ebike".equalsIgnoreCase(type) && latest instanceof EBikeUpdateEvent e) {
                return new EBike(
                        e.bikeId(),
                        e.location().x(),
                        e.location().y(),
                        e.state(),
                        e.batteryLevel()
                );
            } else if ("abike".equalsIgnoreCase(type) && latest instanceof ABikeUpdateEvent a) {
                return new ABike(
                        a.bikeId(),
                        a.location().x(),
                        a.location().y(),
                        a.state(),
                        a.batteryLevel()
                );
            }
            return null;
        });
    }
}