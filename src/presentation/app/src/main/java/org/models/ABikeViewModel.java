package org.models;

import java.awt.*;

public record ABikeViewModel(String id, double x, double y, int batteryLevel, ABikeViewModel.ABikeState state, Color color) {

    public enum ABikeState { AVAILABLE, IN_USE, MAINTENANCE, AUTONOMOUS_MOVING }
    private static final Color DEFAULT_COLOR = Color.BLUE; // Default color

    public ABikeViewModel(String id, double x, double y, int batteryLevel, ABikeViewModel.ABikeState state) {
        this(id, x, y, batteryLevel, state, DEFAULT_COLOR);
    }

    public ABikeViewModel updateBatteryLevel(int batteryLevel) {
        return new ABikeViewModel(id, x, y, batteryLevel, state, color);
    }

    public ABikeViewModel updateState(ABikeViewModel.ABikeState state) {
        return new ABikeViewModel(id, x, y, batteryLevel, state, color);
    }

    public ABikeViewModel updateColor(Color color) {
        return new ABikeViewModel(id, x, y, batteryLevel, state, color);
    }

    public ABikeViewModel updateLocation(double newX, double newY) {
        return new ABikeViewModel(id, newX, newY, batteryLevel, state, color);
    }
}