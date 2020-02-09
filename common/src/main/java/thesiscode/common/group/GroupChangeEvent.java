package thesiscode.common.group;


import thesiscode.common.IEvent;

/**
 * Represents a group change event.
 */
public class GroupChangeEvent implements IEvent {
    private AbstractMulticastGroup changedGroup;
    private IGroupMember changedMember;
    private GroupChangeType changeType;

    /**
     * Creates a new group change event.
     *
     * @param changedGroup  the group which changed
     * @param changedMember the member which changed
     * @param changeType    the type of change that occured
     */
    public GroupChangeEvent(AbstractMulticastGroup changedGroup, IGroupMember changedMember, GroupChangeType
            changeType) {
        this.changedGroup = changedGroup;
        this.changedMember = changedMember;
        this.changeType = changeType;
    }

    public IGroupMember getChangedMember() {
        return changedMember;
    }

    public GroupChangeType getChangeType() {
        return changeType;
    }

    public AbstractMulticastGroup getChangedGroup() {
        return changedGroup;
    }
}
