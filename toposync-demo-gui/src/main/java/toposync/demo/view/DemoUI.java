package toposync.demo.view;

import org.graphstream.graph.Graph;
import toposync.demo.controller.Controller;
import toposync.demo.model.GUI;

import javax.swing.*;
import java.awt.*;

public class DemoUI extends JFrame implements GUI {
    private Container contentPane;

    private TopoPane topoPane;
    private DelaySliderPane delaySliderPane;
    private ButtonsPane buttonsPane;
    private StatusPane statusPane;


    public DemoUI() {
        super("TopoSync-SFC Demo");

        contentPane = getContentPane();

        initFrame();
        initLayout();
        initDelaySlider();
        initTopoPane();
        initButtonsPane();
        initStatusPane();

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

    private void initDelaySlider() {
        delaySliderPane = new DelaySliderPane();
        contentPane.add(delaySliderPane);
    }

    private void initButtonsPane() {
        buttonsPane = new ButtonsPane();
        contentPane.add(buttonsPane);
    }

    private void initStatusPane() {
        statusPane = new StatusPane();
        contentPane.add(statusPane);
    }

    public void setController(Controller controller) {
        buttonsPane.setController(controller);
        delaySliderPane.setController(controller);
    }

    @Override
    public void showError(String error) {
        JOptionPane.showMessageDialog(this, error);
    }

    @Override
    public void topoSyncFetched() {
        buttonsPane.disableTopoSync();
        buttonsPane.enableShortestPath();
        buttonsPane.enableRemove();
        statusPane.setStatus("TopoSync-SFC tree installed", false);
    }

    @Override
    public void shortestPathFetched() {
        buttonsPane.disableShortestPath();
        buttonsPane.enableTopoSync();
        buttonsPane.enableRemove();
        statusPane.setStatus("Shortest-Path-SFC tree installed", false);
    }

    @Override
    public void updateGraph(Graph g) {
        topoPane.refresh(g);
    }

    @Override
    public void updateGraph() {
        topoPane.refresh();
    }

    @Override
    public void reset() {
        buttonsPane.reset();
        statusPane.reset();
    }
}
