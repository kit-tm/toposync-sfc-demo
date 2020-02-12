package toposync.demo;

import org.graphstream.graph.Graph;

public interface EventHandler {
    void showError(String error);

    void showGraph(Graph g);
}
