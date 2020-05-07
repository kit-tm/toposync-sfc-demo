package toposync.demo.view;

import toposync.demo.controller.Controller;

import javax.swing.*;

public class RefreshRemovePane extends JPanel {
    private Controller controller;
    private JButton remove;


    public RefreshRemovePane() {
        initRefreshButton();
        initRemoveButton();
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

    public void setRemoveEnabled(boolean enabled) {
        remove.setEnabled(enabled);
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }
}
