package toposync.demo.model.fetcher;

import java.io.IOException;

public interface TreeRemover {
    void deleteTree() throws IOException, InterruptedException;
}
