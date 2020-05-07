package toposync.demo.model;

import org.graphstream.graph.Graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class State {
    private List<StateObserver> observers;
    private TopologyAugmentation merger;
    private Graph topology;

    public State() {
        this.observers = new ArrayList<>();
        this.merger = new TopologyAugmentation();
    }

    public void addObserver(StateObserver observer) {
        Objects.requireNonNull(observer);
        this.observers.add(observer);
    }

    public void setTopology(Graph topology) {
        if (topology != null) {
            this.topology = topology;
            updateObservers(topology);
        }
    }

    public void setSolution(Graph solution) {
        //if (solution != null && !(solution.getEdgeCount() == 0 && solution.getNodeCount() != 0)) {
        merger.merge(topology, solution);
        updateObservers();
        //}
    }

    private void updateObservers() {
        for (StateObserver observer : observers) {
            observer.updateGraph();
        }
    }

    private void updateObservers(Graph updatedGraph) {
        for (StateObserver observer : observers) {
            observer.updateGraph(updatedGraph);
        }
    }
}
