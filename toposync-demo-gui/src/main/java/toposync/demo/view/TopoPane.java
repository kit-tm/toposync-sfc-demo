package toposync.demo.view;

import org.graphstream.graph.Graph;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class TopoPane extends JPanel {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Graph graph; // the displayed graph
    private SwingViewer currentViewer;
    private ViewPanel topoView;


    public TopoPane() {
        setLayout(new BorderLayout());
    }

    public void createTopoView(Graph g) {
        this.graph = Objects.requireNonNull(g);
        currentViewer = new SwingViewer(g, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        currentViewer.enableAutoLayout();
        topoView = (ViewPanel) currentViewer.addDefaultView(false);
    }

    /**
     * Repaints the view of the old graph
     */
    public void refresh() {
        logger.info("Refreshing topo view");
        setCss(graph);
        SwingUtilities.invokeLater(() -> {
            topoView.revalidate();
            topoView.repaint();
            revalidate();
            repaint();
        });
    }

    /**
     * Creates a new view of the passed graph. The layout is done new, the graph "wobbles"!
     *
     * @param g the new graph to display
     */
    public void refresh(Graph g) {
        logger.info("Refreshing topo view for graph {}", g);

        setCss(g);

        SwingUtilities.invokeLater(() -> {
            if (currentViewer != null) {
                currentViewer.removeView(topoView.getIdView());
            }

            if (topoView != null) {
                remove(topoView);
            }

            createTopoView(g);
            add(topoView);

            revalidate();
            repaint();
        });
    }

    private void setCss(Graph g) {
        g.setAttribute("ui.stylesheet", String.format("url('%s')", "toposync/demo/view/graph_style.css"));
    }
}
