package domain.model;

import ddd.Aggregate;

public record ABike(String id, Location position, int batteryLevel, String state) implements Aggregate<String> {
    @Override
    public String getId() {
        return id;
    }
}
