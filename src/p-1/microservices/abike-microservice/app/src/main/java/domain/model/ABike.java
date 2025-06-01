package domain.model;

import ddd.Aggregate;
import ddd.Entity;

import java.io.Serializable;

public record ABike(String id, P2d position, int batteryLevel, ABikeState state) implements Aggregate<String>, Serializable {
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
    }

    @Override
    public String getId() {
        return this.id;
    }
}