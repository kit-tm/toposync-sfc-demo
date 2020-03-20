package main;

import com.sun.net.httpserver.HttpServer;
import gurobi.GRBEnv;
import gurobi.GRBException;
import main.rest.RESTDispatcher;
import main.rest.SolutionInstaller;
import org.apache.felix.scr.annotations.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.flow.BidirectionalNfvTreeFlowPusher;
import thesiscode.common.flow.INfvTreeFlowPusher;
import thesiscode.common.nfv.placement.deploy.NfvInstantiator;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    public static ApplicationId appId;

    private GRBEnv env;

    private ClientServerLocator clientServerLocator;

    private HttpServer serverREST;

    @Activate
    protected void activate() throws GRBException {
        log.info("Activating..");
        appId = coreService.registerApplication("hiwi.tm.toposync-app");

        setUpClientServerLocator();
        setUpGurobi();
        setUpRESTServer();
    }

    private void setUpClientServerLocator() {
        clientServerLocator = new ClientServerLocator(hostService);
        hostService.addListener(clientServerLocator);
    }

    private void setUpGurobi() throws GRBException {
        log.info("Creating GRB Environment.");
        env = new GRBEnv("/home/felix/toposync_gurobi.log");
        log.info("Created GRB Environment: {}", env);
    }

    private void setUpRESTServer() {
        RequestGenerator requestGenerator = new RequestGenerator(clientServerLocator, topoService);
        INfvTreeFlowPusher pusher = new BidirectionalNfvTreeFlowPusher(appId, flowRuleService);
        NfvInstantiator instantiator = new NfvInstantiator();
        SolutionInstaller installer = new SolutionInstaller(pusher, instantiator, deviceService, hostService);

        try {
            serverREST = HttpServer.create(new InetSocketAddress("localhost", 9355), 0);
            serverREST.createContext("/tree", new RESTDispatcher(requestGenerator, env, installer));
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

        log.info("Deactivated :)");
    }


}
