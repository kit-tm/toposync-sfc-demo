package thesiscode.common.group;

import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.PacketProcessor;

import java.util.Set;

/**
 * Abstraction of group managers. Group managers are responsible for handling group management packets, i.e. packets
 * sent by client to join or leave a group.
 */
public abstract class AbstractGroupManager extends AbstractGroupChanger implements PacketProcessor {
    public abstract Set<AbstractMulticastGroup> getGroups();

    public abstract AbstractMulticastGroup getGroupById(IGroupIdentifier id);

    /**
     * Returns a selector, which matches the group management packets used by the implementing class. Is installed on
     * all switches and forwards the traffic to the controller.
     *
     * @return the traffic selector
     */
    public abstract TrafficSelector getSelector();

    @Override
    public void notifyListeners(GroupChangeEvent event) {
        for (IGroupChangeListener listener : getListeners()) {
            listener.update(event);
        }
    }

}
