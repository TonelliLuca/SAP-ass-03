package application;

import application.ports.*;
import domain.event.*;
import domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UserServiceImpl implements UserServiceAPI {

    private final UserEventStoreRepository eventStore;
    private final UserEventPublisher eventPublisher;
    private final UserProducerPort kafkaProducer;
    private final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    public UserServiceImpl(UserEventStoreRepository eventStore, UserEventPublisher eventPublisher, UserProducerPort kafkaProducer) {
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
        this.kafkaProducer = kafkaProducer;

    }



    // Helper: Rebuild user state from events
    private Optional<User> rebuildUserState(List<Event> events) {
        logger.info("Rebuilding user state for events {}", events);
        User user = null;
        for (Event event : events) {
            if (event instanceof UserCreatedEvent e) {
                user = new User(e.username(), e.type(), 100);
            } else if (event instanceof UserRequestedAbike e && user != null) {
                user = new User(user.getId(), user.getType(), user.getCredit() - 5);
            } else if (event instanceof RechargeCreditEvent e && user != null) {
                // Assuming User has a method to add credit
                user = new User(user.getId(), user.getType(), user.getCredit() + e.amount());
            } else if (event instanceof UserUpdateEvent e && user != null) {
                user = e.user();
            }
        }
        logger.info("User {} rebuilt", user);
        return Optional.ofNullable(user);
    }

    @Override
    public CompletableFuture<User> signIn(Event event) {
        String username = ((UserSignInEvent) event).username();
        return eventStore.getEventsByUsername(username)
            .thenApply(events -> {
                if (events.isEmpty()) {
                    System.err.println("No events found for username: " + username);
                }
                return rebuildUserState(events);
            })
            .thenCompose(optUser -> {
                if (optUser.isPresent()) {
                    return eventStore.saveEvent(event).thenApply(v -> optUser.get());
                } else {
                    CompletableFuture<User> f = new CompletableFuture<>();
                    f.completeExceptionally(new RuntimeException("User not found for username: " + username));
                    return f;
                }
            });
    }

    @Override
    public CompletableFuture<User> signUp(Event event) {
        UserCreatedEvent created = (UserCreatedEvent) event;
        return eventStore.getEventsByUsername(created.username())
            .thenCompose(events -> {
                if (!events.isEmpty()) {
                    CompletableFuture<User> f = new CompletableFuture<>();
                    f.completeExceptionally(new RuntimeException("User already exists"));
                    return f;
                }
                return eventStore.saveEvent(event)
                        .thenApply(v -> new User(created.username(), created.type(), 100))
                        .thenCompose(user -> {
                            UserUpdateEvent updateEvent = new UserUpdateEvent(user);
                            return eventStore.saveEvent(updateEvent)
                                .thenApply(v2 -> {
                                    eventPublisher.publishUserUpdate(user.getId(), updateEvent);
                                    eventPublisher.publishAllUsersUpdates(updateEvent);
                                    kafkaProducer.sendUpdate(updateEvent);
                                    return user;
                                });
                        });
            });
    }

    @Override
    public CompletableFuture<Optional<User>> getUserByUsername(String username) {
        return eventStore.getEventsByUsername(username)
            .thenApply(this::rebuildUserState);
    }

    @Override
    public CompletableFuture<Void> abikeRequested(Event event) {
        if (!(event instanceof UserRequestedAbike requestedAbike)) {
            logger.error("Event {} is not of type UserRequestedAbike requested", event);
            return CompletableFuture.completedFuture(null);
        }
        String userId = requestedAbike.username();
        // Save the abike request event
        return eventStore.saveEvent(event)
            .thenCompose(v -> eventStore.getEventsByUsername(userId))
            .thenApply(this::rebuildUserState)
            .thenCompose(optUser -> {
                if (optUser.isEmpty()) {
                    logger.error("User not found for abike request: {}", userId);
                    return CompletableFuture.completedFuture(null);
                }
                User user = optUser.get();

                UserUpdateEvent updateEvent = new UserUpdateEvent(user);
                eventPublisher.publishUserUpdate(user.getId(), updateEvent);
                eventPublisher.publishAllUsersUpdates(updateEvent);
                kafkaProducer.sendUpdate(updateEvent);
                logger.info("User {} credit decreased by 5 for abike request", userId);
                return CompletableFuture.completedFuture(null);
            });
    }

    @Override
    public CompletableFuture<User> updateUser(Event event) {
        if(!(event instanceof RequestUserUpdateEvent update)) {
            logger.error("Event {} is not of type RequestUserUpdateEvent request", event);
            return CompletableFuture.completedFuture(null);
        }
        String username = update.username();
        return eventStore.getEventsByUsername(username)
            .thenApply(this::rebuildUserState)
            .thenCompose(optUser -> {
                if (optUser.isEmpty()) {
                    CompletableFuture<User> f = new CompletableFuture<>();
                    f.completeExceptionally(new RuntimeException("User not found"));
                    return f;
                }
                UserUpdateEvent updateEvent = new UserUpdateEvent(new User(optUser.get().getId(), optUser.get().getType(), update.credit()));
                return eventStore.saveEvent(event)
                    .thenApply(v -> updateEvent.user())
                    .thenApply(user -> {
                        eventPublisher.publishUserUpdate(user.getId(), updateEvent);
                        eventPublisher.publishAllUsersUpdates(updateEvent);
                        kafkaProducer.sendUpdate(updateEvent);
                        return user;
                    });
            });
    }



    @Override
    public CompletableFuture<User> rechargeCredit(Event event) {
        RechargeCreditEvent recharge = (RechargeCreditEvent) event;
        String username = recharge.username();
        return eventStore.getEventsByUsername(username)
                .thenApply(this::rebuildUserState)
                .thenCompose(optUser -> {
                    if (optUser.isEmpty()) {
                        CompletableFuture<User> f = new CompletableFuture<>();
                        f.completeExceptionally(new RuntimeException("User not found"));
                        return f;
                    }
                    User user = optUser.get();
                    User updated = new User(user.getId(), user.getType(), user.getCredit() + recharge.amount());
                    UserUpdateEvent updateEvent = new UserUpdateEvent(updated);

                    return eventStore.saveEvent(recharge)
                            .thenCompose(v -> eventStore.saveEvent(updateEvent))
                            .thenApply(v -> updated)
                            .thenApply(u -> {
                                eventPublisher.publishUserUpdate(u.getId(), updateEvent);
                                eventPublisher.publishAllUsersUpdates(updateEvent);
                                kafkaProducer.sendUpdate(updateEvent);
                                return u;
                            });
                });
    }


    public CompletableFuture<List<User>> getAllUsers() {
        return eventStore.getAllEvents()
                .thenApply(events -> {
                    logger.info("Fetched {} events from repo", events.size());

                    // Log ogni evento e tipo
                    for (Event event : events) {
                        logger.info("Event: {}, type: {}", event, event == null ? "null" : event.getClass().getSimpleName());
                    }

                    Map<String, List<Event>> groupedByUser = events.stream()
                            .collect(Collectors.groupingBy(event -> {
                                logger.info("Grouping event: {}", event);
                                if (event instanceof UserCreatedEvent) {
                                    return ((UserCreatedEvent) event).username();
                                } else if (event instanceof UserUpdateEvent) {
                                    return ((UserUpdateEvent) event).user().getId();
                                } else if (event instanceof RechargeCreditEvent) {
                                    return ((RechargeCreditEvent) event).username();
                                } else {
                                    logger.error("Unknown event type: {}", event);
                                    return null;
                                }
                            }));

                    logger.info("Grouped events: {}", groupedByUser.keySet());

                    List<User> users = groupedByUser.values().stream()
                            .map(userEvents -> {
                                logger.info("User events before sort: {}", userEvents);
                                userEvents.sort(Comparator.comparing(Event::getTimestamp));
                                logger.info("User events after sort: {}", userEvents);
                                return rebuildUserState(userEvents);
                            })
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList());
                    logger.info("Rebuilt {} users", users.size());
                    return users;
                })
                .exceptionally(ex -> {
                    logger.error("Exception during getAllUsers", ex);
                    return List.of();
                });
    }



    @Override
    public void init() {
        logger.info("Initializing user service");
        this.getAllUsers().thenAccept(users -> {
            users.forEach(user -> {
                logger.info(user.toString());
                UserUpdateEvent event = new UserUpdateEvent(user);
                kafkaProducer.sendUpdate(event);
            });
            logger.info("UserService initialized");
        });
    }
}