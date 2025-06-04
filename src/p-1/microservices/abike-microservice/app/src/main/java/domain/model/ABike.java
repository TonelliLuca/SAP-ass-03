package domain.model;

import ddd.Aggregate;
import ddd.Entity;

import java.io.Serializable;

public record ABike(String id, P2d position, int batteryLevel, ABikeState state, String stationId) implements Aggregate<String>, Serializable {
    public ABike {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        if (batteryLevel < 0 || batteryLevel > 100) {
            throw new IllegalArgumentException("Battery level must be between 0 and 100");
        }
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("Station ID cannot be null or empty");
        }
    }

    @Override
    public String getId() {
        return this.id;
    }
}