package thesiscode.common.group;

import org.onlab.packet.Ip4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.tree.ITree;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.criteria.Criterion;

import java.util.HashSet;
import java.util.Set;

/**
 * Abstraction of a multicast group. A group consists of members and has an id.
 */
public abstract class AbstractMulticastGroup {
    private Logger log = LoggerFactory.getLogger(getClass());


    public abstract Set<Criterion> getMatchCriterionSet();


    /**
     * Sets the current tree for this group.
     *
     * @param tree the new tree
     */
    public abstract void setTree(ITree tree);

    /**
     * Gets the current tree for this group.
     *
     * @return the tree
     */
    public abstract ITree getTree();

    /**
     * Gets the current group members. Should return a COPY of the current group members of this group, because the
     * group members may be modified after returning the group members.
     *
     * @return the set of group members
     */
    public abstract Set<IGroupMember> getGroupMembers();

    /**
     * Gets the identifier of this group.
     *
     * @return the group identifier
     */
    public abstract IGroupIdentifier getId();

    /**
     * Adds a new host to this group. If the host is already a member of this group, nothing happens.
     *
     * @param host the host to add to this group
     * @return true if the host was added, otherwise false
     */
    public abstract boolean addMember(IGroupMember host);

    /**
     * Removes a host from this group. If the host was not a member of this group, nothing happens.
     *
     * @param host the host to remove from this group
     * @return true if the host was removed, otherwise false
     */
    public abstract boolean removeMember(IGroupMember host);

    /**
     * Returns the device Ids of the group members, i.e. the device id of their connect point. Same as invoking
     * {@link ConnectPoint#deviceId()} to each connect point contained
     * in {@link AbstractMulticastGroup#toConnectPoints()}.
     *
     * @return the deviceIds of all group members
     */
    public Set<DeviceId> toDeviceIds() {
        HashSet<DeviceId> identifiers = new HashSet<DeviceId>();
        for (IGroupMember iGroupMember : getGroupMembers()) {
            identifiers.add(iGroupMember.getConnectPoint().deviceId());
        }
        return identifiers;
    }

    /**
     * Returns the connect points of the group members.
     *
     * @return the connect points of all group members
     */
    public Set<ConnectPoint> toConnectPoints() {
        Set<ConnectPoint> connectPoints = new HashSet<>();
        for (IGroupMember mem : getGroupMembers()) {
            connectPoints.add(mem.getConnectPoint());
        }
        return connectPoints;
    }

    /**
     * Returns whether this group is empty or not.
     *
     * @return true if this group is empty, otherwise false
     */
    public boolean isEmpty() {
        return getGroupMembers().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (o.getClass() != this.getClass()) {
            return false;
        }

        AbstractMulticastGroup obj = (AbstractMulticastGroup) o;

        if (getId() == null) {
            return (obj.getId() == null);
        }

        return (obj.getId() == this.getId());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getGroupMembers() == null) ? 0 : getGroupMembers().hashCode());
        return result;
    }

    @Override
    public String toString() {
        return getGroupMembers().toString();
    }

    public IGroupMember getGroupMemberByIp(Ip4Address ip4) {
        for(IGroupMember mem : getGroupMembers()) {
            if(mem.getIpAddress().equals(ip4)) {
                return mem;
            }
        }
        return null;
    }
}
