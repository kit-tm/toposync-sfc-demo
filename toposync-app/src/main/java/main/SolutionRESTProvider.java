package main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SolutionRESTProvider implements HttpHandler {
    private static final String TOPOSYNC_REQUEST_URI = "/tree/toposync-sfc";
    private static final String REF_REQUEST_URI = "tree/shortest-path-sfc";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void handle(HttpExchange httpExchange) {
        String method = httpExchange.getRequestMethod();
        logger.info("received HTTP request: {}", method);

        if (method.equals("POST")) {
            logger.info("Received POST");

            String requestURI = httpExchange.getRequestURI().toString().toLowerCase();
            logger.info("request URI: {}", requestURI);

            if (requestURI.equals(TOPOSYNC_REQUEST_URI)) {
                logger.info("Calculating TopoSync-SFC tree...");
            } else if (requestURI.equals(REF_REQUEST_URI)) {
                logger.info("Calculating REF tree...");
            }
        }
    }
}
