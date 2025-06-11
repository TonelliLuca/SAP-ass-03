package org.models;

public record StationViewModel(String stationId, double x, double y, int capacity, int emptySlots) {
}