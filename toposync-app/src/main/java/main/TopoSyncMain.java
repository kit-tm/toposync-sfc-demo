package main;

import com.sun.net.httpserver.HttpServer;
import gurobi.GRBEnv;
import gurobi.GRBException;
import main.rest.RESTDispatcher;
import org.apache.felix.scr.annotations.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

@Component(immediate = true)
public class TopoSyncMain {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topoService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    public static ApplicationId appId;

    private GRBEnv env;

    private ClientServerLocator clientServerLocator;

    private HttpServer serverREST;

    @Activate
    protected void activate() throws GRBException {
        log.info("Activating..");
        appId = coreService.registerApplication("hiwi.tm.toposync-app");

        clientServerLocator = new ClientServerLocator(hostService);
        hostService.addListener(clientServerLocator);

        RequestGenerator requestGenerator = new RequestGenerator(clientServerLocator, topoService);

        log.info("Creating GRB Environment.");
        env = new GRBEnv("/home/felix/toposync_gurobi.log");
        log.info("Created GRB Environment: {}", env);

        try {
            serverREST = HttpServer.create(new InetSocketAddress("localhost", 9355), 0);
            serverREST.createContext("/tree", new RESTDispatcher(requestGenerator, env));
            serverREST.start();
            log.info("Set up server..");
        } catch (IOException e) {
            log.error("Couldn't set up server.", e);
        }

    }

    @Deactivate
    protected void deactivate() throws GRBException {
        log.info("Stopping server..");
        serverREST.stop(1);

        log.info("Removing listeners..");
        hostService.removeListener(clientServerLocator);

        log.info("Deactivating GRBEnv");
        env.dispose();
        // TODO how to dispose of GRBEnv properly?

        log.info("Deactivated :)");
    }


}
