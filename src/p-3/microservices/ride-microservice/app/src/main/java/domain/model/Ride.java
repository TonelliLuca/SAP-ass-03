package domain.model;

import ddd.Aggregate;

import java.util.Date;
import java.util.Optional;

public class Ride implements Aggregate<String> {
    private final String id;
    private final User user;
    private final Bike bike;
    private final Date startTime;
    private volatile Optional<Date> endTime;
    private volatile boolean ongoing;

    public Ride(String id, User user, Bike bike) {
        this.id = id;
        this.user = user;
        this.bike = bike;
        this.startTime = new Date();
        this.endTime = Optional.empty();
        this.ongoing = false;
    }


    @Override
    public String getId() { return id; }
    public User getUser() { return user; }
    public Bike getBike() { return bike; }
    public boolean isOngoing() { return ongoing; }

    public void start() {
        this.ongoing = true;
        this.bike.setState(BikeState.IN_USE);
    }

    public void end() {
        if (this.ongoing) {
            this.endTime = Optional.of(new Date());
            this.ongoing = false;
        }
    }

    @Override
    public String toString() {
        return String.format("Ride{id='%s', user='%s', bike='%s', ebikeState='%s', position='%s', batteryLevel=%d, ongoing=%s, bike type='%s'}",
                id, user.getId(), bike.getId(), bike.getState(), bike.getLocation().toString(), bike.getBatteryLevel(), ongoing, bike.getType());
    }
}