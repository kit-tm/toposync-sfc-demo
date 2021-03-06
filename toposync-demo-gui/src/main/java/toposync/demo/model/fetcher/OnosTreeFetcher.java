package toposync.demo.model.fetcher;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import toposync.demo.model.GUI;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class OnosTreeFetcher implements TreeFetcher {
    private static final URI SHORTEST_PATH_REQUEST_URI = URI.create("http://127.0.0.1:9355/tree/shortest-path-sfc");
    private static final URI TOPOSYNC_REQUEST_URI = URI.create("http://127.0.0.1:9355/tree/toposync-sfc");
    private static final URI GET_CURRENT_TREE_URI = URI.create("http://127.0.0.1:9355/tree");
    private Logger logger = LoggerFactory.getLogger(getClass());
    private HttpClient solutionClient;
    private HttpRequest topoSyncRequest;
    private HttpRequest shortestPathRequest;
    private HttpRequest currentTreeRequest;
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

        this.currentTreeRequest = HttpRequest.newBuilder().uri(GET_CURRENT_TREE_URI).GET().build();
    }

    @Override
    public Graph fetchCurrentTree() throws IOException, InterruptedException {
        return sendRequest(currentTreeRequest);
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
        final int respCode = response.statusCode();
        switch (respCode) {
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                logger.info("500: Internal Server error..");
                gui.showError(response.body());
                return null;
            case HttpURLConnection.HTTP_OK:
                logger.info("200: Computation was successful...");
                JSONObject respJson = new JSONObject(response.body());
                logger.info("solution json: {}", respJson);
                return solutionJsonToGraph(respJson);
            case HttpURLConnection.HTTP_NOT_FOUND:
                logger.info("404: No solution currently installed!");
                return null;
            default:
                throw new IllegalStateException("Unexpected response code: " + respCode);
        }
    }

    private Graph solutionJsonToGraph(JSONObject respJson) {
        Graph g = new MultiGraph("Solution");
        g.setStrict(false);
        g.setAutoCreate(true);

        JSONArray edges = respJson.getJSONObject("solution").getJSONArray("edges");
        JSONArray placements = respJson.getJSONObject("solution").getJSONArray("placement");

        JSONObject delays = respJson.getJSONObject("delays");

        addEdgesToGraph(g, edges);
        addPlacementsToGraph(g, placements);
        addDelaysToGraph(g, delays);

        final String type = respJson.getJSONObject("solution").getString("type");

        switch (type) {
            case "TOPOSYNC_SFC":
                gui.topoSyncFetched();
                break;
            case "SPT":
                gui.shortestPathFetched();
                break;
            case "TOPOSYNC":
            default:
                throw new IllegalStateException("Unexpected solution type: " + type);
        }

        if (g.getNodeCount() == 0 && g.getEdgeCount() == 0) {
            return null;
        } else {
            return g;
        }
    }

    private void addDelaysToGraph(Graph g, JSONObject delays) {
        for (Map.Entry<String, Object> entry : delays.toMap().entrySet()) {
            final String nodeId = entry.getKey();
            final Object delay = entry.getValue();

            g.getNode(nodeId).setAttribute("ui.label", "hops: " + delay);
        }

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
            vnf.setAttribute("ui.label", "transcoder VNF");
            Edge vnfToPop = g.addEdge("vnf->pop", pop.getId(), vnf.getId(), true);
            vnfToPop.setAttribute("ui.class", "overlayEdge" + i);
            Edge popToVnf = g.addEdge("pop->vnf", vnf.getId(), pop.getId(), true);
            popToVnf.setAttribute("ui.class", "overlayEdge" + (i + 1));
        }
    }
}
