package main;

import com.sun.net.httpserver.HttpServer;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

@Component(immediate = true)
public class TopoSyncMain implements HostListener {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService pktService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    public static ApplicationId appId;

    private static final IpAddress SERVER_ADDRESS = IpAddress.valueOf("10.0.0.1");
    private static final IpAddress CLIENT_1_ADDRESS = IpAddress.valueOf("10.0.0.10");
    private static final IpAddress CLIENT_2_ADDRESS = IpAddress.valueOf("10.0.0.11");

    private Host server;
    private Host client1;
    private Host client2;

    private HttpServer serverREST;

    @Activate
    protected void activate() {
        log.info("Activating..");
        appId = coreService.registerApplication("hiwi.tm.toposync-app");
        hostService.addListener(this);

        try {
            serverREST = HttpServer.create(new InetSocketAddress("localhost", 9355), 0);
            serverREST.createContext("/tree", new SolutionRESTProvider());
            serverREST.start();
            log.info("Set up server..");
        } catch (IOException e) {
            log.error("Couldn't set up server.", e);
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopping server..");
        serverREST.stop(1);
        log.info("Removing listeners..");
        hostService.removeListener(this);
        log.info("Deactivated :)");
    }

    @Override
    public void event(HostEvent hostEvent) {
        final Host host = hostEvent.subject();
        final IpAddress ip = host.ipAddresses().iterator().next();

        if (ip.equals(SERVER_ADDRESS)) {
            log.info("Found server. (IP: {})", SERVER_ADDRESS);
            server = host;
        } else if (ip.equals(CLIENT_1_ADDRESS)) {
            log.info("Found client 1. (IP: {})", CLIENT_1_ADDRESS);
            client1 = host;
        } else if (ip.equals(CLIENT_2_ADDRESS)) {
            log.info("Found client 2. (IP: {})", CLIENT_2_ADDRESS);
            client2 = host;
        }
    }

    @Override
    public boolean isRelevant(HostEvent event) {
        if (event == null || event.subject() == null || event.subject().ipAddresses() == null) {
            log.info("Relevant information was null, not relevant for us.");
            return false;
        }

        Set<IpAddress> ips = event.subject().ipAddresses();
        final int amountOfIps = ips.size();

        if (amountOfIps == 0) {
            log.warn("found host with no IP: {}", event.subject());
            return false;
        } else if (amountOfIps > 1) {
            log.warn("found host with multiple IPs: {}", ips);
            return false;
        }

        return true;
    }
}
