package infrastructure.persistence;

import application.ports.UserEventStoreRepository;
import domain.event.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.event.UserSignInEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MongoUserEventStoreRepository implements UserEventStoreRepository {
    private final MongoClient mongoClient;
    private static final String COLLECTION = "user_events";
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Logger logger = LoggerFactory.getLogger(MongoUserEventStoreRepository.class);

    public MongoUserEventStoreRepository(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public CompletableFuture<Void> saveEvent(Event event) {
        logger.info("Saving event {}", event);
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            JsonObject doc = new JsonObject(mapper.writeValueAsString(event))
                    .put("eventType", event.getClass().getSimpleName());

            // PATCH: Se trovi 'user.id', metti 'user.username'
            if (doc.containsKey("user")) {
                JsonObject userObj = doc.getJsonObject("user");
                if (!userObj.containsKey("username") && userObj.containsKey("id")) {
                    userObj.put("username", userObj.getString("id"));
                    userObj.remove("id");
                    doc.put("user", userObj);
                }
            }

            mongoClient.insert(COLLECTION, doc)
                    .onSuccess(id -> future.complete(null))
                    .onFailure(future::completeExceptionally);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<List<Event>> getEventsByUsername(String username) {
        CompletableFuture<List<Event>> future = new CompletableFuture<>();
        JsonObject query = new JsonObject().put("username", username);
        mongoClient.find(COLLECTION, query)
                .onSuccess(results -> {
                    logger.info("Found {} events for username '{}'", results.size(), username);
                    List<Event> events = results.stream()
                            .map(this::toEvent)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    // Log se trovi documenti malformati
                    long skipped = results.size() - events.size();
                    if (skipped > 0) {
                        logger.warn("Skipped {} malformatted events for username '{}'", skipped, username);
                    }
                    future.complete(events);
                })
                .onFailure(e -> {
                    logger.error("Failed to query events for username '{}': {}", username, e.getMessage());
                    future.completeExceptionally(e);
                });
        return future;
    }

    @Override
    public CompletableFuture<List<Event>> getAllEvents() {
        logger.info("Retrieving all events");
        CompletableFuture<List<Event>> future = new CompletableFuture<>();
        mongoClient.find(COLLECTION, new JsonObject())
                .onSuccess(results -> {
                    logger.info("Mongo query successful, got {} docs", results.size());
                    List<Event> events = results.stream()
                            .map(this::toEvent)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    long skipped = results.size() - events.size();
                    if (skipped > 0) {
                        logger.warn("Skipped {} malformatted events during all-events query", skipped);
                    }
                    logger.info("Mapped {} valid events: {}", events.size(), events.stream().map(e -> e.getClass().getSimpleName()).toList());
                    future.complete(events);
                })
                .onFailure(e -> {
                    logger.error("Mongo query failed", e);
                    future.completeExceptionally(e);
                });
        return future;
    }

    private Event toEvent(JsonObject doc) {
        try {
            String eventType = doc.getString("eventType");
            String json = doc.encode();
            return switch (eventType) {
                case "UserCreatedEvent" -> mapper.readValue(json, domain.event.UserCreatedEvent.class);
                case "UserUpdateEvent" -> mapper.readValue(json, domain.event.UserUpdateEvent.class);
                case "RechargeCreditEvent" -> mapper.readValue(json, domain.event.RechargeCreditEvent.class);
                case "RequestUserUpdateEvent" -> mapper.readValue(json, domain.event.RequestUserUpdateEvent.class);
                case "UserSingInEvent" -> mapper.readValue(json, UserSignInEvent.class);
                case "UserRequestedAbike" -> mapper.readValue(json, domain.event.UserRequestedAbike.class);
                default -> {
                    logger.warn("Unknown eventType '{}' in document: {}", eventType, json);
                    yield null;
                }
            };
        } catch (Exception e) {
            logger.error("Failed to deserialize event: " + doc.encode(), e);
            return null;
        }
    }
}
