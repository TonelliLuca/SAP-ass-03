package domain.model;

import ddd.Aggregate;
import java.io.Serializable;

public class EBike implements Aggregate<String>, Bike, Serializable {

    private final String bikeName;
    private final P2d position;
    private final BikeState state;
    private final int batteryLevel;

    public EBike(String bikeName, P2d position, BikeState state, int batteryLevel) {
        if (state == BikeState.AUTONOMOUS_MOVING) {
            throw new IllegalArgumentException("EBike cannot be in AUTONOMOUS_MOVING state");
        }
        this.bikeName = bikeName;
        this.position = position;
        this.state = state;
        this.batteryLevel = batteryLevel;
    }

    public String getBikeName() {
        return bikeName;
    }

    @Override
    public String getId() {
        return bikeName;
    }

    @Override
    public P2d getPosition() {
        return position;
    }

    @Override
    public BikeState getState() {
        return state;
    }

    @Override
    public int getBatteryLevel() {
        return batteryLevel;
    }

    @Override
    public String getType() {
        return "ebike";
    }



    @Override
    public String toString() {
        return "EBike{" +
                "bikeName='" + bikeName + '\'' +
                ", position=" + position +
                ", state=" + state +
                ", batteryLevel=" + batteryLevel +
                '}';
    }
}