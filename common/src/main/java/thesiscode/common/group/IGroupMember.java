package thesiscode.common.group;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.criteria.Criterion;

import java.util.Set;

/**
 * Abstraction of a group member. In this system, each group member is represented by a MAC address, an IPv4 address
 * and a connect point. Note that host mobility therefore is limited in the current state.
 */
public interface IGroupMember {
    MacAddress getMacAddress();

    Ip4Address getIpAddress();

    ConnectPoint getConnectPoint();

    Host getHost();

    /**
     * Returns match criteria for this group member.
     *
     * @param sourceSet returns source criteria (e.g. IPsrc) if set, else returns destination criteria (e.g. IPdst)
     * @return the criteria
     */
    Set<Criterion> getMatchCriterionSet(boolean sourceSet);
}
