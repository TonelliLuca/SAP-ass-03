package domain.model;

import ddd.Entity;

import java.util.HashSet;

public record Station(String id, P2d location, HashSet<String> dockedBikes, int capacity)
        implements Entity<String> {
    @Override
    public String getId() {
        return this.id;
    }
}