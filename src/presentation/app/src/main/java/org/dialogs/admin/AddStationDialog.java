package org.dialogs.admin;

import org.dialogs.AbstractDialog;
import javax.swing.*;
import java.awt.*;

public class AddStationDialog extends AbstractDialog {
    private final JTextField stationIdField = new JTextField(10);
    private final JTextField xField = new JTextField(10);
    private final JTextField yField = new JTextField(10);
    private final JTextField capacityField = new JTextField(10);
    private boolean confirmed = false;

    public AddStationDialog(JFrame parent) {
        super(parent, "Add Station");
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        addRow("Station ID:", stationIdField);
        addRow("X:", xField);
        addRow("Y:", yField);
        addRow("Capacity:", capacityField);

        setPreferredSize(new Dimension(350, 250));
        pack();
    }

    private void addRow(String label, JTextField field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new Dimension(90, 25));
        row.add(jLabel, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        panel.add(row);
        panel.add(Box.createVerticalStrut(8));
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        if (e.getSource() == confirmButton) {
            confirmed = true;
            dispose();
        } else {
            super.actionPerformed(e);
        }
    }

    public boolean isConfirmed() { return confirmed; }
    public String getStationId() { return stationIdField.getText(); }
    public double getStationX() { return Double.parseDouble(xField.getText()); }
    public double getStationY() { return Double.parseDouble(yField.getText()); }
    public int getCapacity() { return Integer.parseInt(capacityField.getText()); }
}