package toposync.demo.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toposync.demo.controller.Controller;

import javax.swing.*;

public class TreeComputationPane extends JPanel {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Controller controller;

    private JButton topoSync;
    private JButton shortestPath;

    private JLabel statPre;
    private JTextArea status;

    public TreeComputationPane() {
        initTopoSync();
        initShortestPath();
        initStatus();
    }

    private void initStatus() {
        statPre = new JLabel("Status:");
        status = new JTextArea("None.");

        add(statPre);
        add(status);
    }

    private void initTopoSync() {
        topoSync = new JButton("TopoSync-SFC Tree");
        add(topoSync);
        topoSync.addActionListener(e -> {
            setStatus("Computing and installing tree..", true);
            disableShortestPath();
            disableTopoSync();
            onAnotherThread(() -> controller.fetchTopoSyncTree());
        });
    }

    private void initShortestPath() {
        shortestPath = new JButton("Shortest Path-SFC Tree");
        add(shortestPath);
        shortestPath.addActionListener(e -> {
            setStatus("Computing and installing tree..", true);
            disableShortestPath();
            disableTopoSync();
            onAnotherThread(() -> controller.fetchShortestPathTree());
        });
    }

    private void onAnotherThread(Runnable r) {
        Thread t = new Thread(r);
        t.setUncaughtExceptionHandler((th, ex) -> ex.printStackTrace());
        t.start();
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void enableTopoSync() {
        log.debug("enabling toposync button");
        topoSync.setEnabled(true);
    }

    public void enableShortestPath() {
        log.debug("enabling spt button");
        shortestPath.setEnabled(true);
    }

    public void disableTopoSync() {
        log.debug("disabling toposync button");
        topoSync.setEnabled(false);
    }

    public void disableShortestPath() {
        log.debug("disabling spt button");
        shortestPath.setEnabled(false);
    }

    public void setStatus(String statusMessage, boolean showBlinkingCaret) {
        log.debug("changing status");
        status.setText(statusMessage);
        status.getCaret().setVisible(showBlinkingCaret);
    }
}
