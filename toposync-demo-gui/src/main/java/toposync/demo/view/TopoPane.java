package toposync.demo.view;

import org.graphstream.graph.Graph;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class TopoPane extends JPanel {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SwingViewer currentViewer;
    private ViewPanel topoView;


    public TopoPane() {
        setLayout(new BorderLayout());
    }

    public void createTopoView(Graph g) {
        currentViewer = new SwingViewer(g, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        currentViewer.enableAutoLayout();
        topoView = (ViewPanel) currentViewer.addDefaultView(false);
    }

    public void refresh(Graph g) {
        logger.info("Refreshing topo view..");

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
        URL cssPath = ClassLoader.getSystemClassLoader().getResource("graph_style.css");
        if (cssPath == null) {
            logger.warn("Didn't find the graph style sheet. Using fallback default style.");
        } else {
            logger.info("Found stylesheet. Setting graph style accordingly.");
            g.setAttribute("ui.stylesheet", String.format("url('file://%s')", cssPath.getPath()));
        }
        g.nodes().forEach(n -> {
            String uiClass = (String) n.getAttribute("ui.class");
            if (uiClass == null) {
                return;
            }

            switch (uiClass) {
                case "server":
                    n.setAttribute("ui.label", "server");
                    break;
                case "client":
                    n.setAttribute("ui.label", "client");
                    break;
                case "vnf-pop":
                    // TODO label?
                    break;
                case "vnf":
                    n.setAttribute("ui.label", "transcoder");
                    break;
            }
        });
    }
}
