package domain.model;

import ddd.Aggregate;

public class EBike implements Aggregate<String>, Bike{


    private final String id;
    private volatile BikeState state;
    private volatile P2d location;
    private volatile V2d direction;
    private volatile double speed; // Units per simulation tick
    private volatile int batteryLevel; // 0..100

    public EBike(String id, double x, double y, BikeState state, int battery) {
        this.id = id;
        this.state = state;
        this.location = new P2d(x, y);
        this.direction = new V2d(1, 0); // Initial direction
        this.speed = 0; // Default speed
        this.batteryLevel = battery;
    }

    @Override
    public String getId() { return id; }

    @Override
    public synchronized BikeState getState() { return state; }
    @Override
    public synchronized void setState(BikeState state) { this.state = state; }
    @Override
    public synchronized P2d getLocation() { return location; }
    @Override
    public synchronized void setLocation(P2d location) { this.location = location; }
    @Override
    public synchronized V2d getDirection() { return direction; }
    @Override
    public synchronized void setDirection(V2d direction) { this.direction = direction; }
    @Override
    public synchronized int getBatteryLevel() { return batteryLevel; }

    @Override
    public String getType() {
        return "ebike";
    }

    @Override
    public synchronized void decreaseBattery(int amount) {
        this.batteryLevel = Math.max(this.batteryLevel - amount, 0);
        if (this.batteryLevel == 0) {
            this.state = BikeState.MAINTENANCE;
        }
    }

    @Override
    public String toString() {
        return String.format("EBike{id='%s', location=%s, batteryLevel=%d%%, state='%s'}",
                id, location, batteryLevel, state);
    }

}