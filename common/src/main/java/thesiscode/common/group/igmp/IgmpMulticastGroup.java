package thesiscode.common.group.igmp;

import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.group.AbstractMulticastGroup;
import thesiscode.common.group.IGroupIdentifier;
import thesiscode.common.group.IGroupMember;
import thesiscode.common.tree.ITree;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an IGMP multicast group.
 */
public class IgmpMulticastGroup extends AbstractMulticastGroup {
    private static Logger logger = LoggerFactory.getLogger(IgmpMulticastGroup.class);

    private IGroupIdentifier id;
    private Set<IGroupMember> members;

    private ITree distributionTree;

    public IgmpMulticastGroup(IGroupIdentifier id) {
        this.id = id;
        members = Collections.synchronizedSet(new HashSet<>());
    }

    public IgmpMulticastGroup(AbstractMulticastGroup toClone) {
        this.id = toClone.getId();
        this.members = Collections.synchronizedSet(toClone.getGroupMembers());
        this.distributionTree = toClone.getTree();
    }

    @Override
    public Set<Criterion> getMatchCriterionSet() {
        Set<Criterion> criterionSet = new HashSet<>();
        // match group
        switch (id.getType()) {
            case MAC:
                criterionSet.add(Criteria.matchEthDst((MacAddress) id.getValue()));
                break;
            case IPV4:
                criterionSet.add(Criteria.matchEthType(Ethernet.TYPE_IPV4));
                criterionSet.add(Criteria.matchIPDst(((Ip4Address) id.getValue()).toIpPrefix()));
                break;
            case MPLS:
                // TODO
                break;
            case VLAN:
                // TODO
                break;
        }
        return criterionSet;
    }

    @Override
    public void setTree(ITree tree) {
        this.distributionTree = tree;
    }

    @Override
    public ITree getTree() {
        return distributionTree;
    }

    @Override
    public Set<IGroupMember> getGroupMembers() {
        return new HashSet<>(members);
    }

    @Override
    public IGroupIdentifier getId() {
        return id;
    }

    @Override
    public boolean addMember(IGroupMember host) {
        boolean added = members.add(host);
        if (added) {
            logger.debug("Added host {} to group {}.", host, id);
            membersChanged();
        } else {
            logger.debug("Host {} was not added to group {}, because host was already a member.", host, id);
        }
        return added;
    }

    @Override
    public boolean removeMember(IGroupMember host) {
        boolean removed = members.remove(host);
        if (removed) {
            logger.debug("Removed host {} from group {}.", host, id);
            membersChanged();
        } else {
            logger.debug("Host {} was not removed from group {}, because host was not a member.", host, id);
        }
        return removed;
    }

    private void membersChanged() {
        logger.debug("Group's {} members changed: now {}", id, members);
    }

}
