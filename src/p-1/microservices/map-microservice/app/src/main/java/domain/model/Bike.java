package domain.model;

public interface Bike {
    String getId();
    P2d getPosition();
    int getBatteryLevel();
    String getType(); // "ebike" or "abike"
    BikeState getState();
}