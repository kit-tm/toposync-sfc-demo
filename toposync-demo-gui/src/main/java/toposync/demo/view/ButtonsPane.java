package toposync.demo.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toposync.demo.controller.Controller;

import javax.swing.*;
import java.awt.*;

public class ButtonsPane extends JPanel {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Controller controller;

    private JButton remove;
    private JButton topoSync;
    private JButton shortestPath;


    public ButtonsPane() {
        setLayout(new GridLayout(2, 2, 10, 10));

        initRefreshButton();
        initRemoveButton();

        initTopoSync();
        initShortestPath();
    }

    private void initRefreshButton() {
        JButton refresh = new JButton("Refresh");
        add(refresh);
        refresh.addActionListener(e -> {
            controller.fetchTopology();
            controller.fetchCurrentTree();
        });
    }

    private void initRemoveButton() {
        remove = new JButton("Delete tree");
        add(remove);
        remove.addActionListener(e -> {
            controller.deleteTree();
            controller.fetchTopology();
            controller.fetchCurrentTree();
        });
        remove.setEnabled(false);
    }

    private void initTopoSync() {
        topoSync = new JButton("TopoSync-SFC tree");
        add(topoSync);
        topoSync.addActionListener(e -> {
            disableShortestPath();
            disableTopoSync();
            onAnotherThread(() -> controller.fetchTopoSyncTree());
        });
    }

    private void initShortestPath() {
        shortestPath = new JButton("Shortest Path-SFC tree");
        add(shortestPath);
        shortestPath.addActionListener(e -> {
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

    public void enableRemove() {
        remove.setEnabled(true);
    }

    public void disableRemove() {
        remove.setEnabled(true);
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void reset() {
        SwingUtilities.invokeLater(() -> {
            disableRemove();
            enableShortestPath();
            enableTopoSync();
        });
    }
}
