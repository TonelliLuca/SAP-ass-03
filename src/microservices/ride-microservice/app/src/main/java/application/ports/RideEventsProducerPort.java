package application.ports;

import domain.model.Ride;

public interface RideEventsProducerPort {
    void publishRideStart(String bikeId, String userId);
    void publishRideUpdate(Ride ride);
    void publishRideEnd(String bikeId, String userId);
}