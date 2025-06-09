package infrastructure.adapter.web;

import application.ports.Service;
import domain.events.CreateStationEvent;
import infrastructure.utils.MetricsManager;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTStationAdapter {
    private static final Logger logger = LoggerFactory.getLogger(RESTStationAdapter.class);
    private final Service stationService;
    private final Vertx vertx;
    private final MetricsManager metricsManager;

    public RESTStationAdapter(Service stationService, Vertx vertx) {
        this.stationService = stationService;
        this.vertx = vertx;
        this.metricsManager = MetricsManager.getInstance();
    }

    public void configureRoutes(Router router) {
        router.post("/api/stations").handler(this::createStation);
        router.get("/health").handler(this::healthCheck);
        router.get("/metrics").handler(this::metrics);
    }

    private void createStation(RoutingContext ctx) {
        metricsManager.incrementMethodCounter("createStation");
        var timer = metricsManager.startTimer();
        try {
            JsonObject body = ctx.body().asJsonObject();
            String stationId = body.getString("stationId");
            Double x = body.getDouble("x");
            Double y = body.getDouble("y");
            Integer capacity = body.getInteger("capacity");

            if (stationId == null || x == null || y == null || capacity == null) {
                sendError(ctx, 400, "Missing stationId, x, y, or capacity");
                metricsManager.recordError(timer, "createStation", new RuntimeException("Missing stationId, x, y, or capacity"));
                return;
            }

            logger.info("Received create station: stationId={}, x={}, y={}, capacity={}", stationId, x, y, capacity);
            stationService.createStation(new CreateStationEvent(stationId, x, y, capacity))
                .thenAccept(v -> {
                    sendResponse(ctx, 201, new JsonObject().put("message", "Station created"));
                    metricsManager.recordTimer(timer, "createStation");
                })
                .exceptionally(ex -> {
                    handleError(ctx, ex);
                    metricsManager.recordError(timer, "createStation", ex);
                    return null;
                });
        } catch (Exception e) {
            handleError(ctx, e);
            metricsManager.recordError(timer, "createStation", e);
        }
    }

    private void healthCheck(RoutingContext ctx) {
        JsonObject health = new JsonObject()
                .put("status", "UP")
                .put("timestamp", System.currentTimeMillis());
        sendResponse(ctx, 200, health);
    }

    private void metrics(RoutingContext ctx) {
        ctx.response()
                .putHeader("Content-Type", "text/plain")
                .end(metricsManager.getMetrics());
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
}