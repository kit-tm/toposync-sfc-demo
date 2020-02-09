package thesiscode.common.topo;

import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprResources;

import java.util.Map;

public class WrappedPoPVertex extends WrappedVertex {

    private Map<NprNfvTypes.Type, Integer> deploymentCost;
    private Map<NprNfvTypes.Type, Double> hwAccelFactors;
    private Map<NprResources, Integer> resourceCapacity;

    public WrappedPoPVertex(TopologyVertex wrapped, Map<NprNfvTypes.Type, Integer> deploymentCost, Map<NprNfvTypes.Type, Double> hwAccelFactors, Map<NprResources, Integer> resourceCapacity) {
        super(wrapped);
        this.deploymentCost = deploymentCost;
        this.hwAccelFactors = hwAccelFactors;
        this.resourceCapacity = resourceCapacity;
    }

    public int getDeploymentCost(NprNfvTypes.Type vnfType) {
        return deploymentCost.get(vnfType);
    }

    public double getDelay(NprNfvTypes.Type vnfType) {
        return NprNfvTypes.getBaseDelay(vnfType) * hwAccelFactors.get(vnfType);
    }

    public boolean hwAccelerationOffered(NprNfvTypes.Type forVnfType) {
        return (hwAccelFactors.get(forVnfType) != 1.0);
    }

    public int getResourceCapacity(NprResources resource) {
        return resourceCapacity.get(resource);
    }
}
