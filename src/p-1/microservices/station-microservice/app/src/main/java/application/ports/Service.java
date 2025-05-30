package application.ports;

public interface Service {
    void dockBike(String stationId, String bikeId);
    void releaseBike(String stationId, String bikeId);
    void init();
}
