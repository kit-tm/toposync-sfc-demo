package thesiscode.common.group;

import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
public class WrappedHost implements IGroupMember {
    private static Logger log = LoggerFactory.getLogger(WrappedHost.class);

    private Ip4Address ip4;
    private Host host;

    public WrappedHost(Host host) {
        this.host = host;
        Set<IpAddress> ipAddresses = host.ipAddresses();
        if (ipAddresses.isEmpty()) {
            throw new IllegalStateException("Found no ip address of host: " + host);
        } else if (ipAddresses.size() > 1) {
            throw new IllegalStateException("Found more than one ip address of host: " + host);
        }
        this.ip4 = ipAddresses.iterator().next().getIp4Address();
        log.debug("created group member");
    }


    @Override
    public MacAddress getMacAddress() {
        return host.mac();
    }

    @Override
    public Ip4Address getIpAddress() {
        return ip4;
    }

    @Override
    public String toString() {
        return host.mac() + "," + ip4 + " at " + host.location();
    }

    /**
     * The connect point of the switch this group member is connected to.
     *
     * @return the connect point
     */
    @Override
    public ConnectPoint getConnectPoint() {
        return host.location();
    }

    @Override
    public Host getHost() {
        return host;
    }

    @Override
    public Set<Criterion> getMatchCriterionSet(boolean sourceCriterions) {
        Set<Criterion> criterionSet = new HashSet<>();
        criterionSet.add(Criteria.matchEthType(Ethernet.TYPE_IPV4));
        if (sourceCriterions) {
            //criterionSet.add(Criteria.matchEthSrc(host.mac()));
            criterionSet.add(Criteria.matchIPSrc(ip4.toIpPrefix()));
        } else {
            //criterionSet.add(Criteria.matchEthDst(host.mac()));
            criterionSet.add(Criteria.matchIPDst(ip4.toIpPrefix()));
        }
        return criterionSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WrappedHost that = (WrappedHost) o;
        log.debug("connectPoints to compare: {} and {}", host.location(), that.host.location());
        return Objects.equals(host.mac(), that.host.mac()) && Objects.equals(ip4, that.ip4) && Objects.equals(host.location(), host
                .location());
    }

    @Override
    public int hashCode() {
        Objects.requireNonNull(host.mac());
        Objects.requireNonNull(host.location());
        Objects.requireNonNull(ip4);
        return Objects.hash(host.mac(), ip4, host.location());
    }
}
