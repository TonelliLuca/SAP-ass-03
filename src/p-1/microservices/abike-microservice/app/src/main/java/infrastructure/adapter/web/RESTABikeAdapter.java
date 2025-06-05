package infrastructure.adapter.web;

import application.port.ABikeService;
import domain.model.Destination;
import domain.model.P2d;
import infrastructure.utils.MetricsManager;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTABikeAdapter {
    private static final Logger logger = LoggerFactory.getLogger(RESTABikeAdapter.class);
    private final ABikeService abikeService;
    private final MetricsManager metricsManager;

    public RESTABikeAdapter(ABikeService abikeService) {
        this.abikeService = abikeService;
        this.metricsManager = MetricsManager.getInstance();
    }

    public void configureRoutes(Router router) {
        router.post("/api/abikes/create").handler(this::createABike);
        router.post("/api/callAbike").handler(this::callABike); // Add this line
        router.get("/health").handler(this::healthCheck);
        router.get("/metrics").handler(this::metrics);
    }

    private void metrics(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("Content-Type", "text/plain")
                .end(metricsManager.getMetrics());
    }

    private void createABike(RoutingContext ctx) {
        metricsManager.incrementMethodCounter("createABike");
        var timer = metricsManager.startTimer();

        try {
            JsonObject body = ctx.body().asJsonObject();
            String abikeId = body.getString("abikeId");
            String stationId = body.getString("stationId");

            if (abikeId == null || abikeId.trim().isEmpty() || stationId == null || stationId.trim().isEmpty()) {
                metricsManager.recordError(timer, "createABike", new RuntimeException("Invalid id or stationId"));
                sendError(ctx, 400, "Invalid id or stationId");
                return;
            }

            abikeService.createABike(abikeId, stationId)
                    .thenAccept(result -> {
                        sendResponse(ctx, 201, new JsonObject().put("status", "created"));
                        metricsManager.recordTimer(timer, "createABike");
                    })
                    .exceptionally(e -> {
                        metricsManager.recordError(timer, "createABike", e);
                        handleError(ctx, e);
                        return null;
                    });
        } catch (Exception e) {
            handleError(ctx, new RuntimeException("Invalid JSON format"));
        }
    }

    private void healthCheck(RoutingContext ctx) {
        JsonObject health = new JsonObject()
                .put("status", "UP")
                .put("timestamp", System.currentTimeMillis());
        sendResponse(ctx, 200, health);
    }

    private void sendResponse(RoutingContext ctx, int statusCode, Object result) {
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("content-type", "application/json")
                .end(result instanceof String ? (String) result : result.toString());
    }

    private void sendError(RoutingContext ctx, int statusCode, String message) {
        JsonObject error = new JsonObject().put("error", message);
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("content-type", "application/json")
                .end(error.encode());
    }

    private void handleError(RoutingContext ctx, Throwable e) {
        logger.error("Error processing request", e);
        ctx.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new JsonObject()
                        .put("error", e.getMessage())
                        .encode());
    }


    private void callABike(RoutingContext ctx) {
        metricsManager.incrementMethodCounter("callABike");
        var timer = metricsManager.startTimer();

        try {
            JsonObject body = ctx.body().asJsonObject();
            String username = body.getString("username");
            Double x = body.getDouble("x");
            Double y = body.getDouble("y");

            if (username == null || x == null || y == null) {
                metricsManager.recordError(timer, "callABike", new RuntimeException("Invalid input"));
                sendError(ctx, 400, "Invalid input");
                return;
            }

            P2d position = new P2d(x, y);
            Destination destination = new Destination(position, username);

            abikeService.callABike(destination)
                .thenAccept(simulationId -> {
                    sendResponse(ctx, 200, new JsonObject().put("simulationId", simulationId));
                    metricsManager.recordTimer(timer, "callABike");
                })
                .exceptionally(e -> {
                    metricsManager.recordError(timer, "callABike", e);
                    handleError(ctx, e);
                    return null;
                });
        } catch (Exception e) {
            metricsManager.recordError(timer, "callABike", e);
            handleError(ctx, e);
        }
    }
}