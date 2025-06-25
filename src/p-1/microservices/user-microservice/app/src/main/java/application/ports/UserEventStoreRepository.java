package application.ports;

import ddd.Repository;
import domain.event.Event;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for the User Event Store Repository.
 * Provides methods for storing and retrieving user-related events.
 */
public interface UserEventStoreRepository extends Repository {

    /**
     * Saves a user-related event to the event store.
     *
     * @param event the event to be saved
     * @return a CompletableFuture that completes when the event is successfully saved
     */
    CompletableFuture<Void> saveEvent(Event event);

    /**
     * Retrieves all events associated with a specific username.
     *
     * @param username the username for which events are to be retrieved
     * @return a CompletableFuture containing a list of events related to the specified username
     */
    CompletableFuture<List<Event>> getEventsByUsername(String username);

    /**
     * Retrieves all events stored in the event store.
     *
     * @return a CompletableFuture containing a list of all events
     */
    CompletableFuture<List<Event>> getAllEvents();
}