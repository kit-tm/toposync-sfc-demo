package main;

import org.onlab.packet.IpAddress;
import org.onosproject.net.Host;
import org.onosproject.net.host.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class ClientServerLocator implements HostListener {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final IpAddress SERVER_ADDRESS = IpAddress.valueOf("10.0.0.1");
    private static final IpAddress CLIENT_1_ADDRESS = IpAddress.valueOf("10.0.0.10");
    private static final IpAddress CLIENT_2_ADDRESS = IpAddress.valueOf("10.0.0.11");

    private Host server;
    private Host client1;
    private Host client2;

    private HostService hostService;

    public ClientServerLocator(HostService hostService) {
        this.hostService = hostService;
        init();
    }

    /**
     * Fetch server and clients from the hostService. Ensures that server and clients get set even if topo is already
     * there.
     */
    private void init() {
        for (Host host : hostService.getHosts()) {
            if (hasExactlyOneIP(host)) {
                updateHost(host);
            }
        }
    }

    @Override
    public boolean isRelevant(HostEvent event) {
        if (event == null || event.subject() == null) {
            log.info("Necessary information (event or subject) was null, event not relevant for us.");
            return false;
        }

        final Host host = event.subject();
        if (!hasExactlyOneIP(host)) {
            log.info("Host was not relevant.");
            return false;
        }

        return true;
    }

    private boolean hasExactlyOneIP(Host host) {
        final Set<IpAddress> ips = host.ipAddresses();
        final int amountOfIps = ips.size();

        if (amountOfIps == 0) {
            log.warn("found host with no IP: {}", host);
            return false;
        } else if (amountOfIps > 1) {
            log.warn("found host with multiple IPs: {}", host);
            return false;
        }

        return true;
    }


    @Override
    public void event(HostEvent hostEvent) {
        final HostEvent.Type type = hostEvent.type();
        final Host host = hostEvent.subject();

        switch (type) {
            case HOST_REMOVED:
                removeHost(host);
                break;
            case HOST_ADDED: // fall-through
            case HOST_UPDATED: // fall-through
            case HOST_MOVED:
                updateHost(host);
                break;
            default:
                throw new IllegalArgumentException("Illegal hostEvent type: " + type.name());
        }
    }

    public Host getServer() {
        return server;
    }

    public Host getClient1() {
        return client1;
    }

    public Host getClient2() {
        return client2;
    }

    private void removeHost(Host toRemove) {
        final IpAddress ip = toRemove.ipAddresses().iterator().next();

        if (ip.equals(SERVER_ADDRESS)) {
            removeServer();
        } else if (ip.equals(CLIENT_1_ADDRESS)) {
            removeClient1();
        } else if (ip.equals(CLIENT_2_ADDRESS)) {
            removeClient2();
        }
    }

    private void removeServer() {
        server = null;
        log.info("Removed server.");
    }

    private void removeClient1() {
        client1 = null;
        log.info("Removed client1.");
    }

    private void removeClient2() {
        client2 = null;
        log.info("Removed client2.");
    }

    private void updateHost(Host toUpdate) {
        final IpAddress ip = toUpdate.ipAddresses().iterator().next();

        if (ip.equals(SERVER_ADDRESS)) {
            updateServer(toUpdate);
        } else if (ip.equals(CLIENT_1_ADDRESS)) {
            updateClient1(toUpdate);
        } else if (ip.equals(CLIENT_2_ADDRESS)) {
            updateClient2(toUpdate);
        }
    }

    private void updateServer(Host newServer) {
        server = newServer;
        log.info("Updated server. (IP: {})", server);
    }

    private void updateClient1(Host newClient1) {
        client1 = newClient1;
        log.info("Updated client1: {}", client1);
    }

    private void updateClient2(Host newClient2) {
        client2 = newClient2;
        log.info("Updated client2: {}", client2);
    }

}
