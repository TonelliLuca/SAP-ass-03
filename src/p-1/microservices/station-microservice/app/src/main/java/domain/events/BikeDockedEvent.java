package domain.events;

import domain.model.P2d;
import java.io.Serializable;

public record BikeDockedEvent(String stationId, String bikeId, int availableSlots) implements Serializable { }
