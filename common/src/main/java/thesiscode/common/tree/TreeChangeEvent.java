package thesiscode.common.tree;


import thesiscode.common.IEvent;
import thesiscode.common.group.AbstractMulticastGroup;
import thesiscode.common.group.IGroupMember;

/**
 * Event, which is happening when a tree changes.
 */
public class TreeChangeEvent implements IEvent {
    private ITree tree;
    private AbstractMulticastGroup group;
    private IGroupMember mem;


    public TreeChangeEvent(IGroupMember mem, AbstractMulticastGroup group, ITree tree) {
        this.tree = tree;
        this.group = group;
        this.mem = mem;
    }

    public ITree getTree() {
        return tree;
    }

    public AbstractMulticastGroup getGroup() {
        return group;
    }

    public IGroupMember getMember() {
        return this.mem;
    }
}
