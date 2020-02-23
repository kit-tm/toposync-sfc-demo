package toposync.demo.model.fetcher;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toposync.demo.model.GUI;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;

public class OnosTreeFetcher implements TreeFetcher {
    private static final URI SHORTEST_PATH_REQUEST_URI = URI.create("http://127.0.0.1:9355/tree/shortest-path-sfc");
    private static final URI TOPOSYNC_REQUEST_URI = URI.create("http://127.0.0.1:9355/tree/toposync-sfc");
    private Logger logger = LoggerFactory.getLogger(getClass());
    private HttpClient solutionClient;
    private HttpRequest topoSyncRequest;
    private HttpRequest shortestPathRequest;
    private GUI gui;


    public OnosTreeFetcher(GUI gui) {
        this.gui = gui;
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

    @Override
    public Graph fetchTopoSync() throws IOException, InterruptedException {
        return sendRequest(topoSyncRequest);
    }

    @Override
    public Graph fetchShortestPath() throws IOException, InterruptedException {
        return sendRequest(shortestPathRequest);
    }

    private Graph sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> resp = solutionClient.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(resp);
    }

    private Graph handleResponse(HttpResponse<String> response) {
        logger.info("Handling response: {}", response);

        if (response.statusCode() == 500) {
            logger.info("Status Code 500..");
            gui.showError("Computation infeasible");
            return null;
        } else {
            JSONObject respJson = new JSONObject(response.body());
            logger.info("solution json: {}", respJson);
            return solutionJsonToGraph(respJson);
        }
    }

    private Graph solutionJsonToGraph(JSONObject respJson) {
        Graph g = new SingleGraph("Solution");
        g.setStrict(false);
        g.setAutoCreate(true);

        JSONArray edges = respJson.getJSONObject("solution").getJSONArray("edges");
        JSONArray placements = respJson.getJSONObject("solution").getJSONArray("placement");

        addEdgesToGraph(g, edges);
        addPlacementsToGraph(g, placements);

        return g;
    }

    private void addEdgesToGraph(Graph g, JSONArray allEdgesJson) {
        for (int i = 0; i < allEdgesJson.length(); i++) {
            Object current = allEdgesJson.get(i);
            JSONArray overlayEdge = (JSONArray) current;
            addOverlayEdgeToGraph(g, overlayEdge, i);
        }
    }

    private void addOverlayEdgeToGraph(Graph g, JSONArray overlayEdgeJson, int i) {
        for (Object currentEdge : overlayEdgeJson) {
            JSONObject edge = (JSONObject) currentEdge;

            String src = edge.getString("src");
            String dst = edge.getString("dst");

            String edgeId = String.format("solEdge: %s->%s", src, dst);

            Edge graphEdge = g.addEdge(edgeId, src, dst, true);
            graphEdge.setAttribute("ui.class", "overlayEdge" + i);
        }
    }

    private void addPlacementsToGraph(Graph g, JSONArray placements) {
        for (int i = 0; i < placements.length(); i++) {
            Object current = placements.get(i);
            String placement = (String) current;
            Node pop = g.getNode(placement);
            pop.setAttribute("ui.class", "vnf-pop");
            Node vnf = g.addNode("vnf");
            vnf.setAttribute("ui.class", "vnf");
            Edge vnfToPop = g.addEdge("vnf->pop", pop.getId(), vnf.getId(), true);
            vnfToPop.setAttribute("ui.class", "overlayEdge" + i);
            Edge popToVnf = g.addEdge("pop->vnf", vnf.getId(), pop.getId(), true);
            popToVnf.setAttribute("ui.class", "overlayEdge" + (i + 1));
        }
    }
}
