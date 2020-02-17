package main;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import gurobi.GRBEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.placement.solver.*;
import thesiscode.common.nfv.placement.solver.mfcp.used.*;

public class TreeComputationREST implements HttpHandler {
    private static final String TOPOSYNC_REQUEST_URI = "/tree/toposync-sfc";
    private static final String REF_REQUEST_URI = "tree/shortest-path-sfc";

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
        String method = httpExchange.getRequestMethod();
        logger.info("received HTTP request: {}", method);

        if (method.equals("POST")) {
            logger.info("Received POST");

            String requestURI = httpExchange.getRequestURI().toString().toLowerCase();
            logger.info("request URI: {}", requestURI);

            if (requestURI.equals(TOPOSYNC_REQUEST_URI)) {
                logger.info("Calculating TopoSync-SFC tree...");
                computeTopoSyncTree();
            } else if (requestURI.equals(REF_REQUEST_URI)) {
                logger.info("Calculating REF tree...");
                computeRefTree();
            }
        }
    }

    private void computeTopoSyncTree() {
        NfvPlacementRequest request = requestGenerator.createRequest();

        SfcPlacementSolver solver = new TPLSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, true, env, ALPHA, logger);
        NfvPlacementSolution solution = solver.solve(request);
        logger.info("Finished calculating. Solution: {}", solution);
        // TODO send as answer (JSON)
    }

    private void computeRefTree() {
        NfvPlacementRequest request = requestGenerator.createRequest();

        SfcPlacementSolver solver = new RefSfcPlacementSolver(true, env, ALPHA, logger);
        NfvPlacementSolution solution = solver.solve(request);
        // TODO send as answer (JSON)
    }

}
