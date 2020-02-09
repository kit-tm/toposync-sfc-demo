package org.onosproject.nfv.placement.solver;

import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ILinkWeigher;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NfvPlacementRequest {
    private Set<TopologyVertex> vertexes;
    private Set<TopologyEdge> edges;
    private List<NprTraffic> traffic;
    private ILinkWeigher linkWeigher;


    public NfvPlacementRequest(final Set<TopologyVertex> vertexes, final Set<TopologyEdge> edges, final List<NprTraffic> traffic, ILinkWeigher linkWeigher) {
        this.traffic = traffic;
        this.linkWeigher = linkWeigher;
        this.vertexes = new LinkedHashSet<>(vertexes);
        this.edges = new LinkedHashSet<>(edges);
    }


    public List<NprTraffic> getTraffic() {
        return traffic;
    }

    public Set<TopologyVertex> getVertices() {
        return vertexes;
    }

    public Set<TopologyEdge> getEdges() {
        return edges;
    }

    public ILinkWeigher getLinkWeigher() {
        return linkWeigher;
    }
}
