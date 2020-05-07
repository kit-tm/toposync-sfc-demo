package toposync.demo.controller;

import org.graphstream.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toposync.demo.model.GUI;
import toposync.demo.model.State;
import toposync.demo.model.fetcher.TopologyFetcher;
import toposync.demo.model.fetcher.TreeFetcher;
import toposync.demo.model.fetcher.TreeRemover;

import java.io.IOException;
import java.util.Objects;

public class Controller {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private State state;
    private GUI gui;
    private TopologyFetcher topoFetcher;
    private TreeFetcher treeFetcher;
    private TreeRemover treeRemover;


    public Controller(GUI gui, TopologyFetcher topoFetcher, TreeFetcher treeFetcher, TreeRemover remover) {
        this.state = new State();
        this.gui = Objects.requireNonNull(gui);
        state.addObserver(gui);
        this.topoFetcher = Objects.requireNonNull(topoFetcher);
        this.treeFetcher = Objects.requireNonNull(treeFetcher);
        this.treeRemover = remover;
    }

    public void deleteTree() {
        try {
            logger.info("Removing tree..");
            treeRemover.deleteTree();
            logger.info("Removed tree");
            state.setSolution(null);
        } catch (IOException | InterruptedException e) {
            gui.showError("Error when removing tree.");
            logger.error("Error when removing tree.", e);
        }
    }

    public void fetchTopology() {
        try {
            Graph g = topoFetcher.fetchTopology();
            logger.info("Fetched tree with {} nodes, {} edges", g.getNodeCount(), g.getEdgeCount());
            state.setTopology(g);
        } catch (IOException e) {
            gui.showError("Error when fetching topology.");
            logger.error("Error when fetching topology.", e);
        }
    }

    public void fetchTopoSyncTree() {
        try {
            Graph tree = treeFetcher.fetchTopoSync();
            logger.info("Fetched toposync solution with {} nodes, {} edges", tree.getNodeCount(), tree.getEdgeCount());
            state.setSolution(tree);
            gui.topoSyncFetched();
        } catch (IOException | InterruptedException e) {
            gui.showError("Error when fetching toposync solution.");
            logger.error("Error when fetching toposync solution.", e);
        }
    }

    public void fetchShortestPathTree() {
        try {
            Graph tree = treeFetcher.fetchShortestPath();
            logger.info("Fetched spt solution with {} nodes, {} edges", tree.getNodeCount(), tree.getEdgeCount());
            state.setSolution(tree);
            gui.shortestPathFetched();
        } catch (IOException | InterruptedException e) {
            gui.showError("Error when fetching spt solution.");
            logger.error("Error when fetching spt solution.", e);
        }
    }

    public void fetchCurrentTree() {
        try {
            Graph tree = treeFetcher.fetchCurrentTree();
            if (tree != null) {
                logger.info("Fetched currently installed tree with {} nodes, {} edges", tree.getNodeCount(),
                        tree.getEdgeCount());
                state.setSolution(tree);
            } else {
                gui.reset(); // no tree currently installed -> re-activate buttons etc.
            }
        } catch (IOException | InterruptedException e) {
            gui.showError("Error when fetching currently installed tree.");
            logger.error("Error when fetching currently installed tree.", e);
        }
    }
}
