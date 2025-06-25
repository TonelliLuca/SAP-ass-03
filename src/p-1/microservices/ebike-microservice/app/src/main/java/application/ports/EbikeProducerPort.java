package application.ports;

import domain.event.Event;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Port for sending updates to the map microservice adapter.
 * Provides functionality to send e-bike update events to the map service.
 */
public interface EbikeProducerPort {

    /**
     * Sends an update for a single e-bike to the map service.
     * This method is used to transmit e-bike event data to the map microservice,
     * ensuring the map service is updated with the latest e-bike information.
     *
     * @param event the event containing e-bike update details
     */
    void sendUpdate(Event event);

}