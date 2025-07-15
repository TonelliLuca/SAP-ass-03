package application.ports;

    import domain.event.Event;

    import java.util.concurrent.CompletableFuture;

    /**
     * Service interface defines the contract for handling station-related operations
     * and events asynchronously. It provides methods for initialization, event handling,
     * and station creation.
     */
    public interface Service {

        /**
         * Initializes the service and prepares it for operation.
         */
        void init();

        /**
         * Handles the event when an a-bike arrives at a station.
         *
         * @param event The Event representing the arrival of an a-bike to a station.
         */
        void handleABikeArrivedToStation(Event event);

        /**
         * Handles the event when a bike is released from a station.
         *
         * @param event The Event representing the release of a bike from a station.
         */
        void handleBikeReleased(Event event);

        /**
         * Creates a new station asynchronously based on the provided event.
         *
         * @param event The Event containing details for station creation.
         * @return A CompletableFuture that completes when the station creation is done.
         */
        CompletableFuture<Void> createStation(Event event);
    }