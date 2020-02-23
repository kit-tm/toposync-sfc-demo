package toposync.demo.model.fetcher;

import org.graphstream.graph.Graph;

import java.io.IOException;

public interface TopologyFetcher {
    Graph fetchTopology() throws IOException;
}
