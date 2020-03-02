package toposync.demo.model;

import org.graphstream.graph.Graph;

public interface StateObserver {
    void updateGraph(Graph g);

    void updateGraph();
}
