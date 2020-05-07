package main.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import main.ProgressMonitor;
import main.rest.provide.TreeProvider;
import main.view.DeleteProgressWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.placement.deploy.InstantiationException;

import java.io.IOException;

public class RESTDispatcher implements HttpHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TreeComputation treeComputation;
    private SolutionInstaller installer;
    private main.rest.provide.TreeProvider treeProvider;

    public RESTDispatcher(TreeComputation treeComputation, TreeProvider provider, SolutionInstaller installer) {
        this.treeComputation = treeComputation;
        this.treeProvider = provider;
        this.installer = installer;
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            handleInternal(httpExchange);
        } catch (Exception e) { // added here because this is called from another thread
            logger.info("Exception while handling HTTP request", e);
        }
    }

    private void handleInternal(HttpExchange httpExchange) throws IOException, InstantiationException {
        final String method = httpExchange.getRequestMethod();
        logger.info("received HTTP request: {}", method);

        if (method.equals("POST")) {
            String solutionJson = treeComputation.handlePOST(httpExchange);
            treeProvider.setLastSolution(solutionJson);
        } else if (method.equals("GET")) {
            treeProvider.handleGET(httpExchange);
        } else if (method.equals("DELETE")) {

            ProgressMonitor tmp = installer.getProgressMonitor();
            DeleteProgressWindow deleteProgressWindow = new DeleteProgressWindow();
            deleteProgressWindow.init(true, "");
            installer.setProgressMonitor(deleteProgressWindow);

            installer.uninstallOldSolution();
            installer.invalidateSolution();
            treeProvider.setLastSolution(null);
            httpExchange.sendResponseHeaders(200, -1);

            installer.setProgressMonitor(tmp);
        } else {
            logger.warn("unexpected HTTP method: {}", method);
        }
    }
}
