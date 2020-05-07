package toposync.demo.model.fetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OnosTreeRemover implements TreeRemover {
    private static final URI DELETE_CURRENT_TREE_URI = URI.create("http://127.0.0.1:9355/tree");
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private HttpRequest deleteTreeRequest;
    private HttpClient httpClient;


    public OnosTreeRemover() {
        this.httpClient = HttpClient.newHttpClient();
        this.deleteTreeRequest = HttpRequest.newBuilder(DELETE_CURRENT_TREE_URI).DELETE().build();
    }

    @Override
    public void deleteTree() throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(deleteTreeRequest, HttpResponse.BodyHandlers.ofString());

        logger.info("Handling response: {}", response);
        final int respCode = response.statusCode();
        switch (respCode) {
            case HttpURLConnection.HTTP_OK:
                logger.info("200: Removal was successful...");
                break;
            default:
                throw new IllegalStateException("Unexpected response code: " + respCode);
        }
    }
}
