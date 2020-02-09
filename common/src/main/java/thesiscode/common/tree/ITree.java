package thesiscode.common.tree;

import org.onosproject.net.Link;
import org.onosproject.net.flow.TrafficSelector;
import thesiscode.common.group.AbstractMulticastGroup;

import java.util.Set;

/**
 * Represent a tree.
 */
public interface ITree {
    /**
     * Returns all links of the tree.
     *
     * @return the links
     */
    Set<Link> getLinks();

    /**
     * Returns a selector for this tree, which is used in flow rules for this tree.
     *
     * @return the selector
     */
    TrafficSelector getSelector();

    /**
     * Sets the group for which this tree was constructed.
     *
     * @param associatedGroup the group
     */
    void setGroup(AbstractMulticastGroup associatedGroup);
}
