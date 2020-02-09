package thesiscode.common.tree;

import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.TopologyEdge;

import java.util.Set;

public abstract class AbstractTopologyPerSourceTreeAlgorithm {
    abstract Set<TopologyEdge> computeTree(DeviceId source, Set<DeviceId> receivers);
}
