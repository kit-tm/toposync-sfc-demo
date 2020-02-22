package toposync.demo.view;

import toposync.demo.controller.Controller;

import javax.swing.*;

public class TreeComputationPane extends JPanel {
    private Controller controller;

    private JButton topoSync;
    private JButton shortestPath;

    public TreeComputationPane() {
        initTopoSync();
        initShortestPath();
    }

    private void initTopoSync() {
        topoSync = new JButton("TopoSync-SFC Tree");
        add(topoSync);
        topoSync.addActionListener(e -> {
            disableShortestPath();
            disableTopoSync();
            controller.computeTopoSync();
        });
    }

    private void initShortestPath() {
        shortestPath = new JButton("Shortest Path-SFC Tree");
        add(shortestPath);
        shortestPath.addActionListener(e -> {
            disableShortestPath();
            disableTopoSync();
            controller.computeShortestPath();
        });
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void enableTopoSync() {
        topoSync.setEnabled(true);
    }

    public void enableShortestPath() {
        shortestPath.setEnabled(true);
    }

    public void disableTopoSync() {
        topoSync.setEnabled(false);
    }

    public void disableShortestPath() {
        shortestPath.setEnabled(false);
    }
}
