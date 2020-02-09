package fetcher;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

public class OnosTopologyFetcher implements GraphFetcher {
    private static final String FETCH_URL = "http://localhost:8181/onos/v1/links";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private URL fetchUrl = null;

    public OnosTopologyFetcher() throws MalformedURLException {
        fetchUrl = new URL(FETCH_URL);
    }

    @Override
    public Graph fetch() throws IOException {
        String jsonLinks = fetchLinksViaOnosREST();
        Graph g = jsonToGraph(jsonLinks);
        return g;
    }

    private String fetchLinksViaOnosREST() throws IOException {
        HttpURLConnection con = (HttpURLConnection) fetchUrl.openConnection();
        con.setRequestMethod("GET");
        String authString = "karaf:karaf";
        con.setRequestProperty("Authorization",
                               "Basic " + new String(Base64.getEncoder().encode(authString.getBytes())));

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
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
}
