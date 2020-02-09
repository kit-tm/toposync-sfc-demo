package thesiscode.common.tree;

        import org.onosproject.net.Link;
        import thesiscode.common.group.IGroupMember;

        import java.util.Set;

/**
 * Abstraction of a per-source tree.
 */
public interface IPerSourceTree extends ITree {
    /**
     * Returns the source.
     *
     * @return the source of the tree
     */
    IGroupMember getSource();

    Set<Link> getLinks();

    Set<IGroupMember> getReceivers();
}
