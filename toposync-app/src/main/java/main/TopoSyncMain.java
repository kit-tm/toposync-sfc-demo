package main;

import com.sun.net.httpserver.HttpServer;
import gurobi.GRBEnv;
import gurobi.GRBException;
import main.rest.RESTDispatcher;
import main.rest.SolutionInstaller;
import main.rest.SolutionInvalidator;
import main.rest.TreeComputation;
import main.rest.provide.TreeProvider;
import main.view.ProgressWindow;
import org.apache.felix.scr.annotations.*;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.*;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.flow.BidirectionalNfvTreeFlowPusher;
import thesiscode.common.flow.INfvTreeFlowPusher;
import thesiscode.common.nfv.placement.deploy.NfvInstantiator;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

@Component(immediate = true)
public class TopoSyncMain implements PacketProcessor {
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PacketService packetService;

    public static ApplicationId appId;

    private GRBEnv env;

    private ClientServerLocator clientServerLocator;

    private HttpServer serverREST;

    private SolutionInvalidator solutionInvalidator;

    @Activate
    protected void activate() throws GRBException {
        log.info("Activating..");
        appId = coreService.registerApplication("hiwi.tm.toposync-app");

        log.info("Registering for packet processing");
        packetService.addProcessor(this, PacketProcessor.ADVISOR_MAX);

        setUpClientServerLocator();
        setUpGurobi();
        setUpRESTServer();

        topoService.addListener(solutionInvalidator);
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
        ProgressMonitor progressMonitor = new ProgressWindow();
        RequestGenerator requestGenerator = new RequestGenerator(clientServerLocator, topoService);
        INfvTreeFlowPusher pusher = new BidirectionalNfvTreeFlowPusher(appId, flowRuleService);
        NfvInstantiator instantiator = new NfvInstantiator();
        SolutionInstaller installer = new SolutionInstaller(pusher, instantiator, deviceService, hostService,
                progressMonitor);

        try {
            serverREST = HttpServer.create(new InetSocketAddress("localhost", 9355), 0);

            TreeComputation treeComputation = new TreeComputation(requestGenerator, env, installer, progressMonitor);
            TreeProvider provider = new TreeProvider();
            solutionInvalidator = new SolutionInvalidator(provider, installer);
            serverREST.createContext("/tree", new RESTDispatcher(treeComputation, provider, installer));
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
        topoService.removeListener(solutionInvalidator);

        log.info("Deactivating GRBEnv");
        env.dispose();

        log.info("Removing ourself from packet processing.");
        packetService.removeProcessor(this);

        log.info("Deactivated :)");
    }


    /**
     * Proxy ARP, so that hardware clients can respond with ICMP to server ping.
     */
    @Override
    public void process(PacketContext packetContext) {
        final InboundPacket pkt = packetContext.inPacket();
        final ConnectPoint cp = pkt.receivedFrom();
        final Ethernet eth = pkt.parsed();
        final short ethType = eth.getEtherType();
        if (ethType == Ethernet.TYPE_ARP) {
            ARP arp = (ARP) eth.getPayload();
            log.info("Received ARP: {}", arp);
            Ip4Address requestAddress = Ip4Address.valueOf(arp.getTargetProtocolAddress());
            Set<Host> hostsWithRequestedIP = hostService.getHostsByIp(requestAddress);
            if (hostsWithRequestedIP.size() != 1) {
                log.info("Found not exactly one host with requested IP: {}. Returning.", hostsWithRequestedIP);
                return;
            }
            Host requestedHost = hostsWithRequestedIP.iterator().next();
            Ethernet respEth = ARP.buildArpReply(requestAddress, requestedHost.mac(), eth);
            TrafficTreatment treat = DefaultTrafficTreatment.builder().setOutput(cp.port()).build();
            OutboundPacket out = new DefaultOutboundPacket(cp.deviceId(), treat, ByteBuffer.wrap(respEth.serialize()));
            //log.info("Sending ARP reply: {}", respEth);
            packetService.emit(out);
        }
    }
}
