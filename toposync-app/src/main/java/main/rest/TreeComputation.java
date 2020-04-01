package main.rest;

import com.sun.net.httpserver.HttpExchange;
import gurobi.GRBEnv;
import main.RequestGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.placement.deploy.InstantiationException;
import thesiscode.common.nfv.placement.solver.NfvPlacementRequest;
import thesiscode.common.nfv.placement.solver.NfvPlacementSolution;
import thesiscode.common.nfv.placement.solver.OptimizationGoal;
import thesiscode.common.nfv.placement.solver.mfcp.used.RefSfcPlacementSolver;
import thesiscode.common.nfv.placement.solver.mfcp.used.SfcPlacementSolver;
import thesiscode.common.nfv.placement.solver.mfcp.used.TopoSyncSFCPlacementSolver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public class TreeComputation {
    public static final String TOPOSYNC_REQUEST_URI = "/tree/toposync-sfc";
    private static final String REF_REQUEST_URI = "/tree/shortest-path-sfc";
    private static final double ALPHA = 1.0;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private RequestGenerator requestGenerator;
    private GRBEnv env;
    private SolutionInstaller installer;
    private SolutionJsonEncoder solutionJsonEncoder;

    public TreeComputation(RequestGenerator requestGenerator, GRBEnv env, SolutionInstaller installer) {
        this.requestGenerator = requestGenerator;
        this.env = env;
        this.solutionJsonEncoder = new SolutionJsonEncoder();
        this.installer = installer;
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
            try {
                installer.installSolution(solution);
                solutionJson = solutionJsonEncoder.toJson(solution);
                sendSolution(httpExchange, solutionJson);
            } catch (InstantiationException e) {
                e.printStackTrace();
                sendErrorResponse(httpExchange, "VNF instantiation/removal was not possible");
            }
        } else {
            sendErrorResponse(httpExchange, "Model was infeasible, solution == null");
        }

        return solutionJson;
    }

    private void sendErrorResponse(HttpExchange httpExchange, String error) throws IOException {
        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, error.getBytes().length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(error.getBytes());
        os.close();
    }

    private NfvPlacementSolution computeTopoSyncTree() {
        NfvPlacementRequest request = requestGenerator.createRequest();

        SfcPlacementSolver solver = new TopoSyncSFCPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION,
                true, env, ALPHA);

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
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, solutionJson.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(solutionJson.getBytes());
        os.close();
    }
}
