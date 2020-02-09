package thesiscode.common.tree;

import org.onosproject.net.Link;
import org.onosproject.net.flow.TrafficSelector;
import thesiscode.common.group.IGroupMember;

import java.util.List;
import java.util.Set;

public interface IPerLogicalEdgeTree {
    IGroupMember getSource();

    List<Set<Link>> getLinks();

    Set<IGroupMember> getReceivers();

    /**
     * Returns a selector for this tree, which is used in flow rules for this tree.
     *
     * @return the selector
     */
    TrafficSelector getSelector();
}
