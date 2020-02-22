package main.rest;

import com.sun.net.httpserver.HttpExchange;
import gurobi.GRBEnv;
import main.RequestGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.placement.solver.*;
import thesiscode.common.nfv.placement.solver.mfcp.used.*;

import java.io.IOException;
import java.io.OutputStream;

public class TreeComputation {
    public static final String TOPOSYNC_REQUEST_URI = "/tree/toposync-sfc";
    private static final String REF_REQUEST_URI = "/tree/shortest-path-sfc";
    private static final double ALPHA = 1.0;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private RequestGenerator requestGenerator;
    private GRBEnv env;
    private SolutionJsonEncoder solutionJsonEncoder;

    public TreeComputation(RequestGenerator requestGenerator, GRBEnv env) {
        this.requestGenerator = requestGenerator;
        this.env = env;
        this.solutionJsonEncoder = new SolutionJsonEncoder();
    }

    protected String handlePOST(HttpExchange httpExchange) throws IOException {
        String requestURI = httpExchange.getRequestURI().toString().toLowerCase();
        NfvPlacementSolution solution = null;

        if (requestURI.equals(TOPOSYNC_REQUEST_URI)) {
            logger.info("Calculating TopoSync-SFC tree...");
            solution = computeTopoSyncTree();
        } else if (requestURI.equals(REF_REQUEST_URI)) {
            logger.info("Calculating REF tree...");
            solution = computeRefTree();
        } else {
            logger.warn("unexpected request URI: {}", requestURI);
        }

        String solutionJson = null;

        if (solution != null) {
            solutionJson = solutionJsonEncoder.toJson(solution);
            // TODO install solution
            sendSolution(httpExchange, solutionJson);
        } else {
            httpExchange.sendResponseHeaders(500, -1);
        }

        return solutionJson;
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

    private void sendSolution(HttpExchange exchange, String solutionJson) throws IOException {
        exchange.sendResponseHeaders(200, solutionJson.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(solutionJson.getBytes());
        os.close();
    }
}