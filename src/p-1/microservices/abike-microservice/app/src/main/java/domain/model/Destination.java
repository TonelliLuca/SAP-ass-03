package domain.model;

import ddd.Entity;

import java.io.Serializable;

public record Destination(P2d position, String id) implements Serializable, Entity<String> {
    @Override
    public String getId() {
        return this.id;
    }
}
