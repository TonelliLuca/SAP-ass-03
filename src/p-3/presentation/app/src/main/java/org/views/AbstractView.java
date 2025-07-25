package org.views;


import org.models.ABikeViewModel;
import org.models.EBikeViewModel;
import org.models.StationViewModel;
import org.models.UserViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractView extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(AbstractView.class);
    protected JPanel topPanel;
    protected JPanel centralPanel;
    protected JButton logoutButton;
    protected JPanel buttonPanel;
    protected List<StationViewModel> stations;
    protected List<ABikeViewModel> aBikes;
    protected List<EBikeViewModel> eBikes;
    protected UserViewModel actualUser;


    public AbstractView(String title, UserViewModel actualUser) {
        setTitle(title);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        topPanel = new JPanel();
        add(topPanel, BorderLayout.NORTH);

        buttonPanel = new JPanel();
        topPanel.add(buttonPanel, BorderLayout.CENTER);

        centralPanel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintCentralPanel(g);
            }
        };
        add(centralPanel, BorderLayout.CENTER);

        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        topPanel.add(logoutButton, BorderLayout.EAST);

        this.actualUser = actualUser;
        this.eBikes = new CopyOnWriteArrayList<>();
        this.stations = new CopyOnWriteArrayList<>();
        this.aBikes = new CopyOnWriteArrayList<ABikeViewModel>();
    }

    protected void addTopPanelButton(String text, ActionListener actionListener) {
        JButton button = new JButton(text);
        button.addActionListener(actionListener);
        topPanel.add(button);
    }

    protected void updateVisualizerPanel() {
        centralPanel.repaint();
    }

    protected void paintCentralPanel(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.clearRect(0, 0, this.getWidth(), this.getHeight());
        if (actualUser.admin()) {
            paintAdminView(g2);
        } else {
            paintUserView(g2);
        }
        paintStations(g);
    }

    protected void paintAdminView(Graphics2D g2) {
        int centerX = centralPanel.getWidth() / 2;
        int centerY = centralPanel.getHeight() / 2;
        for (EBikeViewModel bike : eBikes) {
            int x = centerX + (int) bike.x();
            int y = centerY - (int) bike.y();
            g2.setColor(bike.color());
            g2.fillOval(x, y, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawString("E-Bike: " + bike.id() + " - battery: " + bike.batteryLevel(), x, y + 35);
            g2.drawString("(x: " + bike.x() + ", y: " + bike.y() + ")", x, y + 50);
            g2.drawString("STATUS: " + bike.state(), x, y + 65);
        }
        for (ABikeViewModel abike : aBikes) {
            int x = centerX + (int) abike.x();
            int y = centerY - (int) abike.y();
            g2.setColor(abike.color());
            g2.fillOval(x, y, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawString("A-Bike: " + abike.id() + " - battery: " + abike.batteryLevel(), x, y + 35);
            g2.drawString("(x: " + abike.x() + ", y: " + abike.y() + ")", x, y + 50);
            g2.drawString("STATUS: " + abike.state(), x, y + 65);
        }
    }

    private void paintUserView(Graphics2D g2) {
        int centerX = centralPanel.getWidth() / 2;
        int centerY = centralPanel.getHeight() / 2;
        int dy = 20;
        for (EBikeViewModel bike : eBikes) {
            int x = centerX + (int) bike.x();
            int y = centerY - (int) bike.y();
            g2.setColor(bike.color());
            g2.fillOval(x, y, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawString("E-Bike: " + bike.id() + " - battery: " + bike.batteryLevel(), x, y + 35);
            g2.drawString("E-Bike: " + bike.id() + " - battery: " + bike.batteryLevel(), 10, dy + 35);
            g2.drawString("(x: " + bike.x() + ", y: " + bike.y() + ")", x, y + 50);
            dy += 15;
        }
        for (ABikeViewModel abike : aBikes) {
            int x = centerX + (int) abike.x();
            int y = centerY - (int) abike.y();
            g2.setColor(abike.color());
            g2.fillOval(x, y, 20, 20);
            g2.setColor(Color.BLACK);
            g2.drawString("A-Bike: " + abike.id() + " - battery: " + abike.batteryLevel(), x, y + 35);
            g2.drawString("(x: " + abike.x() + ", y: " + abike.y() + ")", x, y + 50);
            g2.drawString("STATUS: " + abike.state(), x, y + 65);
        }
        String credit = "Credit: " + actualUser.credit();
        g2.drawString(credit, 10, 20);
        g2.drawString("AVAILABLE EBIKES: ", 10, 35);
        paintUserExtras(g2);
    }

    public void display() {
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }

    public void updateStations(List<StationViewModel> newStations) {
        stations.clear();
        log.info("Updating stations");
        stations.addAll(newStations);
        updateVisualizerPanel();
    }

    public void updateABikes(List<ABikeViewModel> newABikes) {
        aBikes.clear();
        aBikes.addAll(newABikes);
        updateVisualizerPanel();
    }

    protected void paintStations(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int squareSize = 15;
        int spacing = 3;
        int centerX = centralPanel.getWidth() / 2;
        int centerY = centralPanel.getHeight() / 2;

        for (StationViewModel station : stations) {
            int x = centerX + (int) station.x();
            int y = centerY - (int) station.y();
            for (int i = 0; i < station.capacity(); i++) {
                if (i < station.emptySlots()) {
                    g2.setColor(Color.RED); // Available slot
                } else {
                    g2.setColor(Color.GREEN);   // Occupied slot
                }
                g2.fillRect(x + i * (squareSize + spacing), y, squareSize, squareSize);
                g2.setColor(Color.BLACK);
                g2.drawRect(x + i * (squareSize + spacing), y, squareSize, squareSize);
            }
            g2.drawString("Station: " + station.stationId(), x, y - 5);
        }
    }

    protected void paintUserExtras(Graphics2D g2) {
        // Default: do nothing
    }



}