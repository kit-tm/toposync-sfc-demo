package toposync.demo.model;

import org.graphstream.graph.Graph;

import java.util.*;

public class State {
    private List<StateObserver> observers;
    private TopologySolutionMerger merger;
    private Graph topology;
    private Graph solution;
    private Graph merged;

    public State() {
        this.observers = new ArrayList<>();
        this.merger = new TopologySolutionMerger();
    }

    public void addObserver(StateObserver observer) {
        Objects.requireNonNull(observer);
        this.observers.add(observer);
    }

    public void setTopology(Graph topology) {
        this.topology = topology;
        if (topology != null) {
            updateObservers(topology);
        }
    }

    public void setSolution(Graph solution) {
        this.solution = solution;
        if (solution != null) {
            mergeTopoAndSolution();
            updateObservers(merged);
        }
    }

    private void updateObservers(Graph updatedGraph) {
        for (StateObserver observer : observers) {
            observer.updateGraph(updatedGraph);
        }
    }

    private void mergeTopoAndSolution() {
        merged = merger.merge(topology, solution);
    }
}
