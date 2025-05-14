package application.ports;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface UserProducerPort {
    public void sendUpdate(JsonObject update);
    public void sendAllUserUpdate(JsonArray updates);
}
