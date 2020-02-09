package thesiscode.common.group;

/**
 * Interface for identifiers, which identify a group. It is obligatory for
 * implementing classes to override {@link java.lang.Object#equals(Object)} and
 * {@link java.lang.Object#hashCode()} in order to be able to compare groups by
 * their identifiers (see implementation of
 * {@link thesiscode.common.group.AbstractMulticastGroup#equals(Object)}.
 * It is also recommended to override {@link java.lang.Object#toString()} for
 * logging purposes.
 *
 * @author Felix Bachmann
 */
public interface IGroupIdentifier {
    enum GroupIdentifierType {
        IPV4, IPV6, MAC, VLAN, MPLS
    }

    /**
     * Returns the type of this identifier.
     *
     * @return the type
     */
    GroupIdentifierType getType();

    /**
     * Returns the value of this identifier.
     *
     * @return the value
     */
    Object getValue();
}
