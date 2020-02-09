package thesiscode.common.topo;

import org.onosproject.net.topology.TopologyEdge;

public interface ILinkWeigher {
    int getBandwidth(TopologyEdge edge);

    int getDelay(TopologyEdge edge);
}
