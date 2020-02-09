package fetcher;

import org.graphstream.graph.Graph;

import java.io.IOException;

public interface GraphFetcher {

    Graph fetch() throws IOException;

}
