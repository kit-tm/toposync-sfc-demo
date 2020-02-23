package toposync.demo.model.fetcher;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class OnosTopologyFetcher implements TopologyFetcher {
    private static final String LINK_FETCH_URL = "http://localhost:8181/onos/v1/links";
    private static final String HOST_FETCH_URL = "http://localhost:8181/onos/v1/hosts";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Graph fetchTopology() throws IOException {
        String jsonLinks = fetchLinksViaOnosREST();
        Graph g = jsonToGraph(jsonLinks);
        addHostsToGraph(g);
        return g;
    }

    private String fetchLinksViaOnosREST() throws IOException {
        return sendAuthenticatedGetRequest(LINK_FETCH_URL);
    }

    private String sendAuthenticatedGetRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        String authString = "karaf:karaf";
        con.setRequestProperty("Authorization",
                               "Basic " + new String(Base64.getEncoder().encode(authString.getBytes())));

        InputStream inputStream = con.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        return content.toString();
    }

    private Graph jsonToGraph(String linkJson) {
        Graph g = new SingleGraph("Topology");
        g.setStrict(false);
        g.setAutoCreate(true);

        JSONObject json = new JSONObject(linkJson);

        JSONArray allLinks = json.getJSONArray("links");

        for (Object current : allLinks) {
            JSONObject oneLink = (JSONObject) current;

            JSONObject dst = oneLink.getJSONObject("dst");
            JSONObject src = oneLink.getJSONObject("src");

            String dstDpid = dpidOfEndpoint(dst);
            String srcDpid = dpidOfEndpoint(src);

            String linkId = String.format("%s->%s", srcDpid, dstDpid);

            logger.debug(linkId);

            g.addEdge(linkId, srcDpid, dstDpid, false);
        }

        return g;
    }

    private String dpidOfEndpoint(JSONObject link) {
        return link.getString("device");
    }

    private void addHostsToGraph(Graph g) throws IOException {
        String jsonHosts = sendAuthenticatedGetRequest(HOST_FETCH_URL);
        System.out.println(jsonHosts);
        JSONObject json = new JSONObject(jsonHosts);
        JSONArray allHosts = json.getJSONArray("hosts");

        for (Object current : allHosts) {
            JSONObject currentHost = (JSONObject) current;
            String ip = currentHost.getJSONArray("ipAddresses").getString(0);

            JSONObject hostLocation = currentHost.getJSONArray("locations").getJSONObject(0);
            String nodeId = hostLocation.getString("elementId");

            String linkId = String.format("%s<->%s", ip, nodeId);

            Node hostNode = g.addNode(ip);

            boolean isServer = ip.equals("10.0.0.1");

            if (isServer) {
                hostNode.setAttribute("ui.class", "server");
            } else {
                hostNode.setAttribute("ui.class", "client");
            }


            g.addEdge(linkId, ip, nodeId, false);
        }
    }
}
