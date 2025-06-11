package domain.events;

import domain.model.P2d;
import domain.model.Station;

import java.util.Set;

public record StationRegisteredEvent(Station station) implements Event {}
