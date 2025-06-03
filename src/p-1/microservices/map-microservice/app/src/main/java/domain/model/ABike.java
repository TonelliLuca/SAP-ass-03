package domain.model;

import ddd.Aggregate;
import java.io.Serializable;

public class ABike implements Aggregate<String>, Bike, Serializable {
    private final String id;
    private final P2d position;
    private final int batteryLevel;
    private final BikeState state;

    public ABike(String id, P2d position, int batteryLevel, BikeState state) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID cannot be null or empty");
        if (batteryLevel < 0 || batteryLevel > 100) throw new IllegalArgumentException("Battery level must be between 0 and 100");
        if (state == null) throw new IllegalArgumentException("State cannot be null");
        this.id = id;
        this.position = position;
        this.batteryLevel = batteryLevel;
        this.state = state;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public P2d getPosition() {
        return position;
    }

    @Override
    public int getBatteryLevel() {
        return batteryLevel;
    }

    @Override
    public String getType() {
        return "abike";
    }

    @Override
    public BikeState getState() {
        return state;
    }


}