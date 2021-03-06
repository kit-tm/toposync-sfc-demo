package toposync.demo.model.fetcher;

import org.graphstream.graph.Graph;

import java.io.IOException;

public interface TreeFetcher {
    Graph fetchCurrentTree() throws IOException, InterruptedException;

    Graph fetchTopoSync() throws IOException, InterruptedException;

    Graph fetchShortestPath() throws IOException, InterruptedException;
}
