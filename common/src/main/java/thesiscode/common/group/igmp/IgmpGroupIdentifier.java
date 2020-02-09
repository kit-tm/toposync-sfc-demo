package thesiscode.common.group.igmp;

import org.onlab.packet.Ip4Address;
import thesiscode.common.group.IGroupIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A group identifier for IGMP groups.
 */
public class IgmpGroupIdentifier implements IGroupIdentifier {
    private static Logger logger = LoggerFactory.getLogger(IgmpGroupIdentifier.class);

    private Ip4Address groupAddress;

    public IgmpGroupIdentifier(Ip4Address groupAddress) {
        super();
        this.groupAddress = groupAddress;
    }

    @Override
    public GroupIdentifierType getType() {
        return GroupIdentifierType.IPV4;
    }

    public Ip4Address getValue() {
        return this.groupAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((groupAddress == null) ? 0 : groupAddress.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        IgmpGroupIdentifier other = (IgmpGroupIdentifier) obj;
        if (groupAddress == null) {
            return other.groupAddress == null;
        } else {
            return groupAddress.equals(other.groupAddress);
        }
    }

    @Override
    public String toString() {
        return groupAddress.toString();
    }

}
