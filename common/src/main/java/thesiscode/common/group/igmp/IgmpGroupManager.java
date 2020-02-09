package thesiscode.common.group.igmp;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IGMP;
import org.onlab.packet.IGMPGroup;
import org.onlab.packet.IGMPMembership;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.group.AbstractGroupManager;
import thesiscode.common.group.AbstractMulticastGroup;
import thesiscode.common.group.GroupChangeEvent;
import thesiscode.common.group.GroupChangeType;
import thesiscode.common.group.IGroupChangeListener;
import thesiscode.common.group.IGroupIdentifier;
import thesiscode.common.group.IGroupMember;
import thesiscode.common.group.WrappedHost;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Manages groups by inspecting IGMP messages.
 */
public class IgmpGroupManager extends AbstractGroupManager {
    private static Logger logger = LoggerFactory.getLogger(IgmpGroupManager.class);

    private HostService hostService;

    private Set<IGroupChangeListener> listeners;
    private Map<IGroupIdentifier, AbstractMulticastGroup> groupMap;

    public IgmpGroupManager(HostService hostService) {
        this.hostService = hostService;
        listeners = new HashSet<>();
        groupMap = Collections.synchronizedMap(new HashMap<>());
    }


    @Override
    public Set<AbstractMulticastGroup> getGroups() {
        return new HashSet<>(groupMap.values());
    }

    @Override
    public AbstractMulticastGroup getGroupById(IGroupIdentifier id) {
        return new IgmpMulticastGroup(groupMap.get(id));
    }

    @Override
    public TrafficSelector getSelector() {
        return DefaultTrafficSelector.builder()
                                     .matchEthType(Ethernet.TYPE_IPV4)
                                     .matchIPProtocol(IPv4.PROTOCOL_IGMP)
                                     .build();
    }

    @Override
    public void process(PacketContext packetContext) {
        InboundPacket in = packetContext.inPacket();
        Ethernet eth = in.parsed();
        ConnectPoint source = in.receivedFrom();
        MacAddress srcMac = eth.getSourceMAC();
        short etherType = eth.getEtherType();
        if (etherType == Ethernet.TYPE_IPV4) {
            IPv4 ipv4 = (IPv4) eth.getPayload();
            Ip4Address srcIp = Ip4Address.valueOf(ipv4.getSourceAddress());
            if (ipv4.getProtocol() == IPv4.PROTOCOL_IGMP) {

                IGMP igmpPacket = (IGMP) ipv4.getPayload();

                String debugString = String.format("IGMP packet (%s -> %s) at switch %s on port %s", Ip4Address.valueOf(ipv4.getSourceAddress()), Ip4Address
                        .valueOf(ipv4.getDestinationAddress()), source.deviceId().toString(), source.port().toString());

                logger.debug(debugString);

                Set<Host> hosts = hostService.getHostsByIp(srcIp);
                if (hosts.isEmpty()) {
                    throw new IllegalStateException("hosts empty for ip " + srcIp.toString());
                } else if (hosts.size() > 1) {
                    throw new IllegalStateException("more than one host for ip" + srcIp.toString());
                }

                IGroupMember igmpSender = new WrappedHost(hosts.iterator().next());
                logger.debug("wrapped host: {}", igmpSender);

                handleIgmp(igmpPacket, igmpSender);
            }

        }
    }

    /**
     * Handles an IGMP packet
     *
     * @param igmp the packet to handle
     * @param host the host who sent the IGMP packet
     */
    private void handleIgmp(IGMP igmp, IGroupMember host) {
        byte igmpType = igmp.getIgmpType();
        List<IGMPGroup> groups = igmp.getGroups();
        logger.debug("groups: {}", groups);

        for (IGMPGroup group : groups) {
            IGroupIdentifier groupId = new IgmpGroupIdentifier(group.getGaddr().getIp4Address());
            logger.debug("igmp.getIgmpType(): {}", igmp.getIgmpType());
            GroupChangeEvent changeEvent = null;

            switch (igmpType) {
                case IGMP.TYPE_IGMPV3_MEMBERSHIP_REPORT:
                    IGMPMembership report = (IGMPMembership) group;
                    byte recordType = report.getRecordType();
                    boolean sourcesEmpty = report.getSources().isEmpty();
                    if (recordType == IGMPMembership.CHANGE_TO_EXCLUDE_MODE && sourcesEmpty) {
                        // IGMPv3 Join
                        logger.debug("igmpv3 join");
                        changeEvent = handleIgmpJoin(host, groupId);
                    } else if (recordType == IGMPMembership.CHANGE_TO_INCLUDE_MODE && sourcesEmpty) {
                        // IGMPv3 Leave
                        logger.debug("igmpv3 leave");
                        changeEvent = handleIgmpLeave(host, groupId);
                    }
                    break;
                case IGMP.TYPE_IGMPV2_MEMBERSHIP_REPORT: // IGMPv2 Join
                    changeEvent = handleIgmpJoin(host, groupId);
                    break;
                case IGMP.TYPE_IGMPV2_LEAVE_GROUP: // IGMPv2 Leave
                    changeEvent = handleIgmpLeave(host, groupId);
                    break;
                case IGMP.TYPE_IGMPV1_MEMBERSHIP_REPORT: // IGMPv1 Join
                    changeEvent = handleIgmpJoin(host, groupId);
                    break;
                default:
                    break;
            }


            if (changeEvent != null) {
                logger.debug("group {} has changed, notifying listeners: {}", groupId, listeners);
                notifyListeners(changeEvent);
            }
            logger.info("Handled IGMP packet. groups are: {}", groupMap);
        }
    }

    /**
     * Handles an IGMP join.
     *
     * @param joiningHost the host which is joining
     * @param groupId     the identifier of the group, which the host joins
     * @return if the call had any effect on the existing groups a {@link GroupChangeEvent} is returned, else null
     */
    private GroupChangeEvent handleIgmpJoin(IGroupMember joiningHost, IGroupIdentifier groupId) {
        logger.debug("Handling join for group {}. Existing groups are {}.", groupId, groupMap.keySet());
        if (groupMap.containsKey(groupId)) {
            logger.debug("The group {} was already existent.", groupId);
            AbstractMulticastGroup existingGroup = groupMap.get(groupId);
            boolean added = existingGroup.addMember(joiningHost);
            if (added) {
                return new GroupChangeEvent(existingGroup, joiningHost, GroupChangeType.JOIN);
            } else {
                return null;
            }
        } else {
            IgmpMulticastGroup newGroup = new IgmpMulticastGroup(groupId);
            newGroup.addMember(joiningHost);
            groupMap.put(groupId, newGroup);
            logger.debug("The group {} was newly created.", groupId);
            return new GroupChangeEvent(newGroup, joiningHost, GroupChangeType.JOIN);
        }
    }

    /**
     * Handles an IGMP leave.
     *
     * @param leavingHost the host which is leaving
     * @param groupId     the identifier of the group, which the host joins
     * @return if the call had any effect on the existing groups a {@link GroupChangeEvent} is returned, else null
     */
    private GroupChangeEvent handleIgmpLeave(IGroupMember leavingHost, IGroupIdentifier groupId) {
        logger.debug("Handling leave for group {}. Existing groups are {}.", groupId, groupMap.keySet());
        if (groupMap.containsKey(groupId)) {
            AbstractMulticastGroup existingGroup = groupMap.get(groupId);
            boolean removed = existingGroup.removeMember(leavingHost);
            if (existingGroup.isEmpty()) {
                groupMap.remove(groupId);
                logger.debug("The group {} was deleted, because it is empty.", groupId);
            }
            if (removed) {
                return new GroupChangeEvent(existingGroup, leavingHost, GroupChangeType.LEAVE);
            } else {
                return null;
            }
        } else {
            logger.debug("The group did not exist, not leaving.");
            return null;
        }
    }


    @Override
    public Set<IGroupChangeListener> getListeners() {
        return listeners;
    }

    @Override
    public void addListener(IGroupChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IGroupChangeListener listener) {
        listeners.remove(listener);
    }

}
