package org.views;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.dialogs.user.RechargeCreditDialog;
import org.dialogs.user.StartRideDialog;
import org.dialogs.user.CallAbikeDialog;
import org.models.ABikeViewModel;
import org.models.EBikeViewModel;
import org.models.UserViewModel;
import org.verticles.UserVerticle;
import java.util.List;
import javax.swing.*;
import java.awt.*;

public class UserView extends AbstractView {

    private final UserVerticle verticle;
    private final Vertx vertx;
    private JButton rideButton;
    private boolean isRiding = false;
    private boolean abikeCallActive = false;
    private boolean abikeArrived = false;
    private JButton callABikeButton;
    private Double calledX = null;
    private Double calledY = null;

    public UserView(UserViewModel user, Vertx vertx) {
        super("User View", user);
        this.vertx = vertx;
        this.verticle = new UserVerticle(vertx, user.username());
        setupView();
        this.verticle.init();
        observeAvailableBikes();
        observeUser();
        observeRideUpdate();
        observeStations();
        observeABikeArrivedToUser();
        refreshView();

    }

    private void setupView() {
        topPanel.setLayout(new FlowLayout());
        callABikeButton = new JButton("Call ABike");
        callABikeButton.addActionListener(event -> handleCallAbikeButton());
        buttonPanel.add(callABikeButton);
        rideButton = new JButton("Start Ride");
        rideButton.addActionListener(e -> toggleRide());
        buttonPanel.add(rideButton);

        addTopPanelButton("Recharge Credit", e -> {
            SwingUtilities.invokeLater(() -> {
                RechargeCreditDialog rechargeCreditDialog = new RechargeCreditDialog(UserView.this, vertx, actualUser);
                rechargeCreditDialog.setVisible(true);
            });
        });
        updateButtonStates();
    }

    private void toggleRide() {
        if (isRiding) {
            stopRide();
        } else {
            startRide();
        }
    }

    public void setRiding(boolean isRiding) {
        this.isRiding = isRiding;
        updateRideButtonState();
        updateButtonStates();
    }

    private void updateRideButtonState() {
        rideButton.setText(isRiding ? "Stop Ride" : "Start Ride");
    }

    private void startRide() {
        SwingUtilities.invokeLater(() -> {
            StartRideDialog startRideDialog = new StartRideDialog(UserView.this, vertx, actualUser);
            startRideDialog.setVisible(true);
            refreshView();
        });
    }

    private void stopRide() {
        SwingUtilities.invokeLater(() -> {
            JsonObject rideDetails = new JsonObject().put("username", actualUser.username());
            vertx.eventBus().request("user.ride.stop." + actualUser.username(), rideDetails, ar -> {
                SwingUtilities.invokeLater(() -> {
                    if (ar.succeeded()) {
                        JOptionPane.showMessageDialog(this, "Ride stopped");
                        setRiding(false);
                    } else {
                        JOptionPane.showMessageDialog(this, "Error stopping ride: " + ar.cause().getMessage());
                    }
                });
            });
            refreshView();
        });
    }

    private void observeRideUpdate() {
        vertx.eventBus().consumer("user.ride.update." + actualUser.username(), message -> {
            JsonObject update = (JsonObject) message.body();
            if (update.containsKey("rideStatus")) {
                String status = update.getString("rideStatus");
                if ("started".equals(status)) {
                    setRiding(true);
                    abikeArrived = false;
                    abikeCallActive = false;
                } else if ("stopped".equals(status)) {
                    setRiding(false);
                    abikeArrived = false;
                    abikeCallActive = false;
                }
                updateButtonStates();
                refreshView();
            }
        });
    }


    private void observeAvailableBikes() {
        vertx.eventBus().consumer("user.bike.update." + actualUser.username(), message -> {
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

    public void observeUser(){
        vertx.eventBus().consumer("user.update." + actualUser.username(), message -> {
            JsonObject update = (JsonObject) message.body();

            JsonObject userJson = update.getJsonObject("user");
            String username = userJson.getString("username");
            int credit = userJson.getInteger("credit");
            if (username.equals(actualUser.username())) {
                actualUser = actualUser.updateCredit(credit);
            }
            refreshView();

        });
    }

    private void refreshView() {
        updateVisualizerPanel();
    }

    private void log(String msg) {
        System.out.println("[UserView-"+actualUser.username()+"] " + msg);
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

    private void openCallAbikeDialog() {
        SwingUtilities.invokeLater(() -> {
            CallAbikeDialog dialog = new CallAbikeDialog(this, vertx, actualUser);
            dialog.setVisible(true);
            if (dialog.isConfirmed()) {
                calledX = dialog.getXCoord();
                calledY = dialog.getYCoord();
                abikeCallActive = true;
                abikeArrived = false;
                updateButtonStates();
                JsonObject req = new JsonObject()
                        .put("username", actualUser.username())
                        .put("x", calledX)
                        .put("y", calledY);
                vertx.eventBus().request("user.call.abike." + actualUser.username(), req, ar -> {
                    if (ar.succeeded()) {
                        JOptionPane.showMessageDialog(this, "Abike called successfully");
                    } else {
                        JOptionPane.showMessageDialog(this, "Error calling abike: " + ar.cause().getMessage());
                        abikeCallActive = false;
                        calledX = null;
                        calledY = null;
                        updateButtonStates();
                    }
                });
                refreshView();
            }
        });
    }

    @Override
    protected void paintUserExtras(Graphics2D g2) {
        if (calledX != null && calledY != null) {
            int centerX = centralPanel.getWidth() / 2;
            int centerY = centralPanel.getHeight() / 2;
            int x = centerX + calledX.intValue();
            int y = centerY - calledY.intValue();
            g2.setColor(Color.RED);
            g2.fillOval(x - 5, y - 5, 10, 10);
            g2.setColor(Color.BLACK);
            g2.drawString("User Call", x + 10, y);
        }

    }


    private void observeABikeArrivedToUser() {
        vertx.eventBus().consumer("user.abike.event." + actualUser.username(), message -> {
            JsonObject obj = (JsonObject) message.body();
            if ("ABikeArrivedToUser".equals(obj.getString("event"))) {
                abikeArrived = true;
                abikeCallActive = false;
                calledX = null;
                calledY = null;
                isRiding = true;
                updateButtonStates();
                refreshView();
            }
        });
    }

    private void handleCallAbikeButton() {
        if (abikeCallActive && !abikeArrived) {
            // Cancel the ABike call via backend
            JsonObject req = new JsonObject()
                    .put("username", actualUser.username());
            vertx.eventBus().request("user.call.abike.cancel." + actualUser.username(), req, ar -> {
                SwingUtilities.invokeLater(() -> {
                    if (ar.succeeded()) {
                        abikeCallActive = false;
                        calledX = null;
                        calledY = null;
                        updateButtonStates();
                        refreshView();
                        JOptionPane.showMessageDialog(this, "ABike call cancelled");
                    } else {
                        JOptionPane.showMessageDialog(this, "Error cancelling ABike call: " + ar.cause().getMessage());
                    }
                });
            });
        } else {
            openCallAbikeDialog();
        }
    }

    private void updateButtonStates() {
        if (isRiding) {
            callABikeButton.setText("Call ABike");
            callABikeButton.setEnabled(false);
            rideButton.setText("Stop Ride");
            rideButton.setEnabled(true);
        } else if (abikeCallActive && !abikeArrived) {
            callABikeButton.setText("Delete Call");
            callABikeButton.setEnabled(true);
            rideButton.setText("Start Ride");
            rideButton.setEnabled(false);
        } else if (abikeArrived) {
            callABikeButton.setText("Call ABike");
            callABikeButton.setEnabled(false);
            rideButton.setText("Start Ride");
            rideButton.setEnabled(true);
        } else {
            callABikeButton.setText("Call ABike");
            callABikeButton.setEnabled(true);
            rideButton.setText("Start Ride");
            rideButton.setEnabled(true);
        }
    }
}