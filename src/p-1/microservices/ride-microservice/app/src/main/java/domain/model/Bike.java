package domain.model;

public interface Bike {
    String getId();
    P2d getLocation();
    int getBatteryLevel();
    String getType(); // "ebike" or "abike"
    BikeState getState();
    V2d getDirection();
    void setState(BikeState state);
    void setDirection(V2d direction);
    void setLocation(P2d location);
    void decreaseBattery(int amount);

}