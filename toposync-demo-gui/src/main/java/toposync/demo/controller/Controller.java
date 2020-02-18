package toposync.demo.controller;

import org.graphstream.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toposync.demo.EventHandler;
import toposync.demo.fetcher.GraphFetcher;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;

public class Controller {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private EventHandler handler;
    private GraphFetcher fetcher;

    private static final URI SHORTEST_PATH_REQUEST_URI = URI.create("http://127.0.0.1:9355/tree/shortest-path-sfc");
    private static final URI TOPOSYNC_REQUEST_URI = URI.create("http://127.0.0.1:9355/tree/toposync-sfc");
    private HttpClient solutionClient;
    private HttpRequest topoSyncRequest;
    private HttpRequest shortestPathRequest;


    public Controller(EventHandler handler, GraphFetcher fetcher) {
        this.handler = handler;
        this.fetcher = fetcher;
        this.solutionClient = HttpClient.newHttpClient();
        this.topoSyncRequest = HttpRequest.newBuilder()
                                          .uri(TOPOSYNC_REQUEST_URI)
                                          .POST(HttpRequest.BodyPublishers.noBody())
                                          .build();

        this.shortestPathRequest = HttpRequest.newBuilder()
                                              .uri(SHORTEST_PATH_REQUEST_URI)
                                              .POST(HttpRequest.BodyPublishers.noBody())
                                              .build();
    }

    public void fetchGraph() {
        try {
            Graph g = fetcher.fetch();
            logger.info("Fetched graph with {} nodes, {} edges", g.getNodeCount(), g.getEdgeCount());
            handler.showGraph(g);
        } catch (IOException e) {
            handler.showError("Could not fetch Graph");
            logger.error("Could not fetch Graph!", e);
        }
    }

    public void computeTopoSync() {
        sendRequest(topoSyncRequest);
        handler.topoSyncComputed();
    }

    public void computeShortestPath() {
        sendRequest(shortestPathRequest);
        handler.shortestPathComputed();
    }

    private void sendRequest(HttpRequest request) {
        // TODO return json response
        solutionClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                      .thenApply(HttpResponse::body)
                      .thenAccept(logger::info)
                      .join();
    }
}
