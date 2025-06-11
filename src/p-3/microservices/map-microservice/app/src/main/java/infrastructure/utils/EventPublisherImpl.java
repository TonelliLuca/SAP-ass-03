package infrastructure.utils;

import application.ports.EventPublisher;
import domain.model.Bike;
import domain.model.EBike;
import domain.model.Station;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class EventPublisherImpl implements EventPublisher {
    private final Vertx vertx;

    public EventPublisherImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void publishBikesUpdate(List<Bike> bikes) {
        JsonArray bikesJson = new JsonArray();
        bikes.forEach(bike -> bikesJson.add(convertBikeToJson(bike)));
        vertx.eventBus().publish("bikes.update", bikesJson.encode());
    }

    @Override
    public void publishUserBikesUpdate(List<Bike> bikes, String username) {
        JsonArray bikesJson = new JsonArray();
        bikes.forEach(bike -> bikesJson.add(convertBikeToJson(bike)));
        vertx.eventBus().publish(username, bikesJson.encode());
    }

    @Override
    public void publishUserAvailableBikesUpdate(List<Bike> bikes) {
        JsonArray bikesJson = new JsonArray();
        bikes.forEach(bike -> bikesJson.add(convertBikeToJson(bike)));
        vertx.eventBus().publish("available_bikes", bikesJson.encode());
    }

    @Override
    public void publishStopRide(String username) {
        JsonObject json = new JsonObject();
        json.put("rideStatus", "stopped");
        vertx.eventBus().publish("ride.stop." + username, json.encode());
    }

    private JsonObject convertBikeToJson(Bike bike) {
        JsonObject json = new JsonObject();
        json.put("id", bike.getId());
        json.put("type", bike.getType());
        json.put("position", new JsonObject()
                .put("x", bike.getPosition().x())
                .put("y", bike.getPosition().y()));
        json.put("state", bike.getState().toString());
        json.put("batteryLevel", bike.getBatteryLevel());
        return json;
    }

    @Override
    public void publishStationsUpdate(List<Station> stations) {
        JsonArray stationsJson = new JsonArray();
        stations.forEach(station -> stationsJson.add(convertStationToJson(station)));
        vertx.eventBus().publish("stations.update", stationsJson.encode());
    }



    private JsonObject convertStationToJson(Station station) {
        JsonObject json = new JsonObject();
        json.put("stationId", station.getId());
        json.put("position", new JsonObject()
                .put("x", station.getLocation().x())
                .put("y", station.getLocation().y()));
        json.put("capacity", station.getCapacity());
        json.put("availableCapacity", station.getAvailableCapacity());
        return json;
    }

    @Override
    public void publishABikeArrivedToUser(String userId, String abikeId) {
        JsonObject json = new JsonObject()
            .put("event", "ABikeArrivedToUser")
            .put("abikeId", abikeId);
        vertx.eventBus().publish(userId, json.encode());
    }
}