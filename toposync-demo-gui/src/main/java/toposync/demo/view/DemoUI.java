package toposync.demo.view;

import org.graphstream.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toposync.demo.EventHandler;
import toposync.demo.controller.Controller;

import javax.swing.*;
import java.awt.*;

public class DemoUI extends JFrame implements EventHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Controller controller;

    private Container contentPane;
    private TopoPane topoPane;
    private TreeComputationPane treeComputationPane;


    public DemoUI() {
        super("TopoSync-SFC Demo");

        contentPane = getContentPane();

        initFrame();
        initLayout();
        initTopoPane();
        initRefreshButton();
        initTreeComputationPane();

        setVisible(true);
    }

    private void initFrame() {
        setSize(new Dimension(800, 800));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void initLayout() {
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    }

    private void initTopoPane() {
        topoPane = new TopoPane();
        contentPane.add(topoPane);
    }

    private void initRefreshButton() {
        JButton refresh = new JButton("Refresh Topology");
        refresh.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPane.add(refresh);
        refresh.addActionListener(e -> controller.fetchGraph());
    }

    private void initTreeComputationPane() {
        treeComputationPane = new TreeComputationPane();
        contentPane.add(treeComputationPane);
    }

    public void setController(Controller controller) {
        this.controller = controller;
        treeComputationPane.setController(controller);
    }


    @Override
    public void showError(String error) {
        JOptionPane.showMessageDialog(this, error);
    }

    @Override
    public void showGraph(Graph g) {
        topoPane.refresh(g);
    }

    @Override
    public void topoSyncComputed() {
        treeComputationPane.enableShortestPath();
        // TODO
    }

    @Override
    public void shortestPathComputed() {
        treeComputationPane.enableTopoSync();
        // TODO
    }
}
