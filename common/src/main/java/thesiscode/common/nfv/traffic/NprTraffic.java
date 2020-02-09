package thesiscode.common.nfv.traffic;

import org.onosproject.net.topology.TopologyVertex;

import java.util.List;
import java.util.Set;

public class NprTraffic {
    private List<NprNfvTypes.Type> sfc;
    private TopologyVertex ingressNode;
    private Set<TopologyVertex> egressNodes;
    private int demand = 1;

    public NprTraffic(List<NprNfvTypes.Type> sfc, TopologyVertex ingressNode, Set<TopologyVertex> egressNodes, int demand) {
        this.sfc = sfc;
        this.ingressNode = ingressNode;
        this.egressNodes = egressNodes;
        this.demand = demand;
    }

    public NprTraffic(List<NprNfvTypes.Type> sfc, TopologyVertex ingressNode, Set<TopologyVertex> egressNodes) {
        this.sfc = sfc;
        this.ingressNode = ingressNode;
        this.egressNodes = egressNodes;
    }

    public List<NprNfvTypes.Type> getSfc() {
        return sfc;
    }

    public TopologyVertex getIngressNode() {
        return ingressNode;
    }

    public Set<TopologyVertex> getEgressNodes() {
        return egressNodes;
    }

    public double getDemand() {
        return demand;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append(ingressNode.toString()).append("->{");
        for (TopologyVertex egress : egressNodes) {
            sb.append(',').append(egress.toString());
        }
        sb.append('}');
        if (sfc != null && sfc.size() > 0) {
            sb.append("sfc={");
            for (NprNfvTypes.Type type : sfc) {
                sb.append(',').append(type.name());
            }
            sb.append("}");
        }
        sb.append(",demand=").append(demand);
        return sb.toString();
    }
}
