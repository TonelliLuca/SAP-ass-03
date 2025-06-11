package org.dialogs.user;

import io.vertx.core.Vertx;
import org.dialogs.AbstractDialog;
import org.models.UserViewModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CallAbikeDialog extends AbstractDialog {
    private JTextField xField;
    private JTextField yField;
    private boolean confirmed = false;
    private double xCoord;
    private double yCoord;

    public CallAbikeDialog(JFrame parent, Vertx vertx, UserViewModel user) {
        super(parent, "Call Abike");
        setupDialog();
    }

    private void setupDialog() {
        xField = new JTextField();
        yField = new JTextField();
        addField("X Coordinate:", xField);
        addField("Y Coordinate:", yField);
        confirmButton.setText("Call");
        cancelButton.setText("Cancel");
        pack();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (e.getSource() == confirmButton) {
            try {
                xCoord = Double.parseDouble(xField.getText());
                yCoord = Double.parseDouble(yField.getText());
                confirmed = true;
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid coordinates");
            }
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public double getXCoord() {
        return xCoord;
    }

    public double getYCoord() {
        return yCoord;
    }
}