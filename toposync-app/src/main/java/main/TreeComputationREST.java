package main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import gurobi.GRBEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.placement.solver.*;
import thesiscode.common.nfv.placement.solver.mfcp.used.*;

import java.io.IOException;
import java.io.OutputStream;

public class TreeComputationREST implements HttpHandler {
    public static final String TOPOSYNC_REQUEST_URI = "/tree/toposync-sfc";
    private static final String REF_REQUEST_URI = "/tree/shortest-path-sfc";

    private static final double ALPHA = 1.0;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private RequestGenerator requestGenerator;
    private GRBEnv env;

    public TreeComputationREST(RequestGenerator requestGenerator, GRBEnv env) {
        this.env = env;
        this.requestGenerator = requestGenerator;
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
        String method = httpExchange.getRequestMethod();
        logger.info("received HTTP request: {}", method);

        if (method.equals("POST")) {
            logger.info("Received POST");

            String requestURI = httpExchange.getRequestURI().toString().toLowerCase();
            logger.info("request URI: {}", requestURI);

            NfvPlacementSolution solution = null;

            if (requestURI.equals(TOPOSYNC_REQUEST_URI)) {
                logger.info("Calculating TopoSync-SFC tree...");
                solution = computeTopoSyncTree();
            } else if (requestURI.equals(REF_REQUEST_URI)) {
                logger.info("Calculating REF tree...");
                solution = computeRefTree();
            }

            String response = solutionToString(solution);
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private NfvPlacementSolution computeTopoSyncTree() {
        NfvPlacementRequest request = requestGenerator.createRequest();

        SfcPlacementSolver solver = new TPLSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, true, env, ALPHA, logger);

        NfvPlacementSolution solution = solver.solve(request);
        logger.info("Finished calculating TopoSync-SFC solution: {}", solution);
        return solution;
    }

    private NfvPlacementSolution computeRefTree() {
        NfvPlacementRequest request = requestGenerator.createRequest();

        SfcPlacementSolver solver = new RefSfcPlacementSolver(true, env, ALPHA, logger);
        NfvPlacementSolution solution = solver.solve(request);
        logger.info("Finished calculating REF solution: {}", solution);
        return solution;
    }

    private String solutionToString(NfvPlacementSolution solution) {
        // TODO to json (maybe factor out to Converter Class)
        String json = "";
        if (solution == null) {
            json = "could not compute tree";
        }
        return json;
    }

}
