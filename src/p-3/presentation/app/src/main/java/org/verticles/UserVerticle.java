package org.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(UserVerticle.class);
    private final WebClient webClient;
    private final HttpClient httpClient;
    private WebSocket userWebSocket;
    private WebSocket bikeWebSocket;
    private final Vertx vertx;
    private final String username;
    private static final int PORT = 8088;
    private static final String ADDRESS = "localhost";

    public UserVerticle(Vertx vertx, String username) {
        this.vertx = vertx;
        this.webClient = WebClient.create(vertx);
        this.httpClient = vertx.createHttpClient();
        this.username = username;
    }

    public void init() {
        vertx.deployVerticle(this).onSuccess(id -> {
            System.out.println("UserVerticle deployed successfully with ID: " + id);
            setupWebSocketConnections();
        }).onFailure(err -> {
            System.out.println("Failed to deploy UserVerticle: " + err.getMessage());
        });
    }

    private void setupWebSocketConnections() {
        httpClient.webSocket(PORT, ADDRESS, "/USER-MICROSERVICE/observeUser/" + username)
            .onSuccess(ws -> {
                System.out.println("Connected to user updates WebSocket: " + username);
                userWebSocket = ws;
                ws.textMessageHandler(this::handleUserUpdate);
                ws.exceptionHandler(err -> {
                    System.out.println("WebSocket error: " + err.getMessage());
                });
            }).onFailure(err -> {
                System.out.println("Failed to connect to user updates WebSocket: " + err.getMessage());
            });

        httpClient.webSocket(PORT, ADDRESS, "/MAP-MICROSERVICE/observeUserBikes?username=" + username)
            .onSuccess(ws -> {
                        System.out.println("Connected to user bikes updates WebSocket");
                        bikeWebSocket = ws;
                        ws.textMessageHandler(message -> {
                            log.info(message);
                            try {
                                if (message.contains("event")) {
                                    vertx.eventBus().publish("user.abike.event." + username, new JsonObject(message));
                                } else if (message.contains("rideStatus")) {
                                    vertx.eventBus().publish("user.ride.update." + username, new JsonObject(message));
                                } else {
                                    vertx.eventBus().publish("user.bike.update." + username, new JsonArray(message));
                                }
                            } catch (Exception e) {
                                log.error("Failed to parse WebSocket message: " + message, e);
                            }
                        });
                    });

        httpClient.webSocket(8088, "localhost", "/MAP-MICROSERVICE/observeStations")
                .onSuccess(ws -> {
                    System.out.println("Connected to stations WebSocket");
                    ws.textMessageHandler(message -> {
                        log.info("Received station update: " + message);
                        JsonArray rawArr = new JsonArray(message);
                        JsonArray objArr = new JsonArray();
                        for (int i = 0; i < rawArr.size(); i++) {
                            Object el = rawArr.getValue(i);
                            if (el instanceof String) {
                                objArr.add(new JsonObject((String) el));
                            } else if (el instanceof JsonObject) {
                                objArr.add(el);
                            }
                        }
                        vertx.eventBus().publish("station.update", objArr);
                    });
                });
    }

    private void handleUserUpdate(String message) {
        JsonObject update = new JsonObject(message);
        JsonObject userJson = update.getJsonObject("user");
        String username = userJson.getString("username");
        vertx.eventBus().publish("user.update." + username, update);
    }

    @Override
    public void start() {

        vertx.eventBus().consumer("user.update.recharge" + username, message -> {
            JsonObject creditDetails = (JsonObject) message.body();
            webClient.patch(PORT, ADDRESS, "/USER-MICROSERVICE/api/users/" + username + "/recharge")
                .sendJsonObject(creditDetails, ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        message.reply(ar.result().bodyAsJsonObject());
                    } else {
                        message.fail(500, "Failed to recharge credit: " +
                            (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                    }
                });
        });

        vertx.eventBus().consumer("user.ride.start." + username, message -> {
            JsonObject rideDetails = (JsonObject) message.body();
            webClient.post(PORT, ADDRESS, "/RIDE-MICROSERVICE/startRide")
                    .sendJsonObject(rideDetails, ar -> {
                        if (ar.succeeded()) {
                            if (ar.result().statusCode() == 200) {
                                message.reply(ar.result().bodyAsString());
                            } else {
                                JsonObject errorResponse = ar.result().bodyAsJsonObject();
                                String errorMessage = errorResponse != null ?
                                        errorResponse.getString("error") : "Unknown error";
                                message.fail(ar.result().statusCode(), errorMessage);
                            }
                        } else {
                            message.fail(500, "Failed to start ride: " +
                                    (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                        }
                    });
        });

        vertx.eventBus().consumer("user.ride.stop." + username, message -> {
            JsonObject rideDetails = (JsonObject) message.body();
            webClient.post(PORT, ADDRESS, "/RIDE-MICROSERVICE/stopRide")
                .sendJsonObject(rideDetails, ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        message.reply(ar.result().bodyAsString());
                    } else {
                        message.fail(500, "Failed to stop ride: " +
                            (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                    }
                });
        });

        vertx.eventBus().consumer("user.call.abike." + username, message -> {
            JsonObject req = (JsonObject) message.body();
            log.info("Received call abike request: " + req);
            webClient.post(PORT, ADDRESS, "/ABIKE-MICROSERVICE/api/callAbike")
                .sendJsonObject(req, ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        message.reply(ar.result().bodyAsString());
                    } else {
                        message.fail(500, "Failed to call abike: " +
                            (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                    }
                });
        });

        vertx.eventBus().consumer("user.call.abike.cancel." + username, message -> {
            JsonObject req = (JsonObject) message.body();
            log.info("Received cancel abike request: " + req);
            webClient.post(PORT, ADDRESS, "/ABIKE-MICROSERVICE/api/cancelCall")
                .sendJsonObject(req, ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        message.reply(ar.result().bodyAsString());
                    } else {
                        message.fail(500, "Failed to cancel abike call: " +
                            (ar.cause() != null ? ar.cause().getMessage() : "Unknown error"));
                    }
                });
        });
    }

    @Override
    public void stop() {
        if (userWebSocket != null) {
            userWebSocket.close();
        }
        if (bikeWebSocket != null) {
            bikeWebSocket.close();
        }

    }
}
