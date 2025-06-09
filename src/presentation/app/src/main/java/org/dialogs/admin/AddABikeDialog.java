package org.dialogs.admin;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.dialogs.AbstractDialog;
import org.models.StationViewModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class AddABikeDialog extends AbstractDialog {

    private JTextField idField;
    private JComboBox<String> stationComboBox;
    private final Vertx vertx;
    private final List<StationViewModel> stations;

    public AddABikeDialog(JFrame parent, Vertx vertx, List<StationViewModel> stations) {
        super(parent, "Adding A-Bike");
        this.vertx = vertx;
        this.stations = stations;
        setupDialog();
    }

    private void setupDialog() {
        idField = new JTextField();
        stationComboBox = new JComboBox<>();
        for (StationViewModel station : stations) {
            if (station.emptySlots() > 0)
                stationComboBox.addItem(station.stationId());
        }
        addField("A-Bike Name (ID):", idField);
        addField("Station:", stationComboBox);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getSource() == confirmButton) {
            String id = idField.getText();
            String stationId = (String) stationComboBox.getSelectedItem();

            if (stationId == null || id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter an A-Bike ID and select a station.");
                return;
            }

            JsonObject bikeDetails = new JsonObject()
                    .put("abikeId", id)
                    .put("stationId", stationId);

            vertx.eventBus().request("admin.abike.create", bikeDetails, reply -> {
                if (reply.succeeded()) {
                    JOptionPane.showMessageDialog(this, "A-Bike added successfully");
                } else {
                    JOptionPane.showMessageDialog(this, "Error adding A-Bike: " + reply.cause().getMessage());
                }
                dispose();
            });
        }
    }
}