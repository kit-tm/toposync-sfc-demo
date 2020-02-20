package main.rest;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public class TreeProvider {
    private static final String CURRENT_TREE_URI = "/tree";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String lastSolutionJson;


    protected void handleGET(HttpExchange httpExchange) throws IOException {
        final String requestURI = httpExchange.getRequestURI().toString().toLowerCase();

        if (requestURI.equals(CURRENT_TREE_URI)) {
            sendResponse(httpExchange);
        } else {
            logger.warn("unexpected request URI: {}", requestURI);
        }
    }

    protected void setLastSolution(String solution) {
        this.lastSolutionJson = solution;
    }

    private void sendResponse(HttpExchange httpExchange) throws IOException {
        if (lastSolutionJson != null) {
            sendLastSolution(httpExchange);
        } else {
            httpExchange.sendResponseHeaders(404, -1);
        }
    }

    private void sendLastSolution(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(200, lastSolutionJson.getBytes().length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(lastSolutionJson.getBytes());
        os.close();
    }
}
