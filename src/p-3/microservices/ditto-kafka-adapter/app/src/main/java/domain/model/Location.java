package domain.model;

import ddd.ValueObject;

public record Location(double x, double y) implements ValueObject {
}
