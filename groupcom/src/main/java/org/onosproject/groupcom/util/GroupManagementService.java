package org.onosproject.groupcom.util;

import thesiscode.common.group.AbstractMulticastGroup;
import thesiscode.common.group.IGroupIdentifier;

import java.util.Set;

public interface GroupManagementService {
    Set<AbstractMulticastGroup> getActiveGroups();

    AbstractMulticastGroup getGroupById(IGroupIdentifier id);
}
