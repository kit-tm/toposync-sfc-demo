package view;

import fetcher.GraphFetcher;
import fetcher.OnosTopologyFetcher;
import org.graphstream.graph.Graph;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

public class DemoUI extends JFrame {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private GraphFetcher topoFetcher;
    private View topoView;

    public DemoUI() throws IOException {
        super("TopoSync-SFC Demo");
        topoFetcher = new OnosTopologyFetcher();

        initFrame();
        initLayout();
        initTopoPane();

        // TODO add button to refresh topo

        setVisible(true);
    }

    private void initFrame() {
        setSize(new Dimension(800, 800));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void initLayout() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
    }

    private void initTopoPane() throws IOException {
        Graph graph = topoFetcher.fetch();
        Viewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        viewer.enableAutoLayout();
        topoView = viewer.addDefaultView(false);
        add((JPanel) topoView);

        URL cssPath = ClassLoader.getSystemClassLoader().getResource("graph_style.css");

        if (cssPath == null) {
            logger.warn("Didn't find the graph style sheet. Using fallback default style.");
        } else {
            logger.info("Found stylesheet. Setting graph style accordingly.");
            graph.setAttribute("ui.stylesheet", String.format("url('file://%s')", cssPath.getPath()));
        }
    }
}
