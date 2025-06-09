package org.views;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.dialogs.admin.AddEBikeDialog;
import org.dialogs.admin.RechargeBikeDialog;
import org.dialogs.admin.AddStationDialog;
import org.models.ABikeViewModel;
import org.models.EBikeViewModel;
import org.models.UserViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verticles.AdminVerticle;
import org.dialogs.admin.AddABikeDialog;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AdminView extends AbstractView {

    private static final Logger log = LoggerFactory.getLogger(AdminView.class);
    private final List<UserViewModel> userList = new CopyOnWriteArrayList<>();
    private final AdminVerticle verticle;
    private final Vertx vertx;

    public AdminView(UserViewModel user, Vertx vertx) {
        super("Admin View", user);
        this.vertx = vertx;
        this.verticle = new AdminVerticle(vertx);
        this.verticle.init();
        setupView();
        observeAllBikes();
        observeAllUsers();
        observeStations();
        refreshView();
    }

    private void setupView() {
        topPanel.setLayout(new FlowLayout());

        JButton addBikeButton = new JButton("Add E-Bike");
        addBikeButton.addActionListener(e -> {
            AddEBikeDialog addEBikeDialog = new AddEBikeDialog(AdminView.this, vertx);
            addEBikeDialog.setVisible(true);
        });
        topPanel.add(addBikeButton);

        JButton addABikeButton = new JButton("Add A-Bike");
        addABikeButton.addActionListener(e -> {
            AddABikeDialog addABikeDialog = new AddABikeDialog(AdminView.this, vertx, stations);
            addABikeDialog.setVisible(true);
        });
        topPanel.add(addABikeButton);

        JButton rechargeBikeButton = new JButton("Recharge Bike");
        rechargeBikeButton.addActionListener(e -> {
            RechargeBikeDialog rechargeBikeDialog = new RechargeBikeDialog(AdminView.this, vertx);
            rechargeBikeDialog.setVisible(true);
        });
        topPanel.add(rechargeBikeButton);
        JButton addStationButton = new JButton("Create Station");
        addStationButton.addActionListener(e -> {
            AddStationDialog dialog = new AddStationDialog(AdminView.this);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                JsonObject stationDetails = new JsonObject()
                        .put("stationId", dialog.getStationId())
                        .put("x", dialog.getStationX())
                        .put("y", dialog.getStationY())
                        .put("capacity", dialog.getCapacity());
                log.info(stationDetails.encode());
                vertx.eventBus().request("admin.station.create", stationDetails, ar -> {
                    if (ar.succeeded()) {
                        log("Station created: " + ar.result().body());
                    } else {
                        log("Failed to create station: " + ar.cause().getMessage());
                    }
                });
            }
        });
        topPanel.add(addStationButton);
    }

    private void observeAllBikes() {
        vertx.eventBus().consumer("admin.bike.update", message -> {
            JsonArray update = (JsonArray) message.body();
            eBikes.clear();
            aBikes.clear();
            for (int i = 0; i < update.size(); i++) {
                JsonObject bikeObj = null;
                Object element = update.getValue(i);
                if (element instanceof String) {
                    bikeObj = new JsonObject((String) element);
                } else if (element instanceof JsonObject) {
                    bikeObj = (JsonObject) element;
                }
                if (bikeObj != null) {
                    String type = bikeObj.getString("type", "").toLowerCase();
                    String id = bikeObj.getString("bikeName", bikeObj.getString("id"));
                    Integer batteryLevel = bikeObj.getInteger("batteryLevel");
                    String stateStr = bikeObj.getString("state");
                    JsonObject location = bikeObj.getJsonObject("position");
                    Double x = location.getDouble("x");
                    Double y = location.getDouble("y");

                    if ("ebike".equals(type)) {
                        EBikeViewModel.EBikeState state = EBikeViewModel.EBikeState.valueOf(stateStr);
                        eBikes.add(new EBikeViewModel(id, x, y, batteryLevel, state));
                    } else if ("abike".equals(type)) {
                        ABikeViewModel.ABikeState state = ABikeViewModel.ABikeState.valueOf(stateStr);
                        aBikes.add(new ABikeViewModel(id, x, y, batteryLevel, state));
                    }
                } else {
                    log("Invalid bike data: " + element);
                }
            }
            refreshView();
        });
    }


    private void observeAllUsers() {
        vertx.eventBus().consumer("admin.user.update", message -> {
            JsonObject update = (JsonObject) message.body();
            String username = update.getString("username");
            String type = update.getString("type");
            Integer credit = update.getInteger("credit");

            if (type.equals("USER") && userList.stream().noneMatch(user -> user.username().equals(username))) {
                UserViewModel user = new UserViewModel(username, credit , false);
                userList.add(user);
            } else if (type.equals("USER")) {
                System.out.println("Updating user: " + username);
                userList.stream()
                        .filter(u -> u.username().equals(username))
                        .findFirst()
                        .ifPresent(u -> {
                            userList.remove(u);
                            userList.add(new UserViewModel(username, credit, false));
                        });
            }
            System.out.println("Received user update: " + update);
            refreshView();
        });
    }

    @Override
    protected void paintAdminView(Graphics2D g2) {
        super.paintAdminView(g2);
        printAllUsers(g2);
    }

    private void printAllUsers(Graphics2D g2) {
        int dy = 20;
        g2.drawString("ALL USERS: ", 10, dy);
        dy += 15;
        for (UserViewModel user : userList) {
            g2.drawString("User ID: " + user.username() + " - Credit: " + user.credit(), 10, dy);
            dy += 15;
        }
    }

    public void refreshView() {
        updateVisualizerPanel();
    }

    private void log(String msg) {
        System.out.println("[AdminView] " + msg);
    }

    private void observeStations() {
        vertx.eventBus().consumer("station.update", message -> {
            JsonArray arr = (JsonArray) message.body();
            List<org.models.StationViewModel> stationList = new java.util.ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.getJsonObject(i);
                String id = obj.getString("stationId");
                JsonObject pos = obj.getJsonObject("position");
                double x = pos.getDouble("x");
                double y = pos.getDouble("y");
                int capacity = obj.getInteger("capacity");
                int emptySlots = obj.getInteger("availableCapacity");
                stationList.add(new org.models.StationViewModel(id, x, y, capacity, emptySlots));
            }
            updateStations(stationList);
        });
    }
}