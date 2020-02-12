package toposync.demo.controller;

import org.graphstream.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toposync.demo.EventHandler;
import toposync.demo.fetcher.GraphFetcher;

import java.io.IOException;

public class Controller {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private EventHandler handler;
    private GraphFetcher fetcher;

    public Controller(EventHandler handler, GraphFetcher fetcher) {
        this.handler = handler;
        this.fetcher = fetcher;
    }

    public void fetchGraph() {
        Graph g = null;
        try {
            g = fetcher.fetch();
        } catch (IOException e) {
            handler.showError("Could not fetch Graph!");
            logger.error("Could not fetch Graph!", e);
        }
        logger.info("Fetched graph with {} nodes, {} edges", g.getNodeCount(), g.getEdgeCount());

        handler.showGraph(g);
    }
}
