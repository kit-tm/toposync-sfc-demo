package main.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import gurobi.GRBEnv;
import main.RequestGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RESTDispatcher implements HttpHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TreeComputation treeComputation;
    private TreeProvider treeProvider;

    public RESTDispatcher(RequestGenerator requestGenerator, GRBEnv env) {
        this.treeComputation = new TreeComputation(requestGenerator, env);
        this.treeProvider = new TreeProvider();
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            handleInternal(httpExchange);
        } catch (Exception e) { // added here because this is called from another thread
            logger.info("Exception while handling HTTP request", e);
        }
    }

    private void handleInternal(HttpExchange httpExchange) throws IOException {
        final String method = httpExchange.getRequestMethod();
        logger.info("received HTTP request: {}", method);

        if (method.equals("POST")) {
            String solutionJson = treeComputation.handlePOST(httpExchange);
            treeProvider.setLastSolution(solutionJson);
        } else if (method.equals("GET")) {
            treeProvider.handleGET(httpExchange);
        } else {
            logger.warn("unexpected HTTP method: {}", method);
        }
    }
}
