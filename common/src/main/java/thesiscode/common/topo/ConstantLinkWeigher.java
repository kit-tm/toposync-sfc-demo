package thesiscode.common.topo;

import org.onosproject.net.topology.TopologyEdge;

public class ConstantLinkWeigher implements ILinkWeigher {
    private static final int DEFAULT_BANDWIDTH_VALUE = 1;
    private static final int DEFAULT_DELAY_VALUE = 1;

    private int bandwidthValue;
    private int delayValue;

    public ConstantLinkWeigher() {
        this.bandwidthValue = DEFAULT_BANDWIDTH_VALUE;
        this.delayValue = DEFAULT_DELAY_VALUE;
    }

    public ConstantLinkWeigher(int bandwidthValue, int delayValue) {
        this.bandwidthValue = bandwidthValue;
        this.delayValue = delayValue;
    }

    public int getBandwidth(TopologyEdge edge) {
        return bandwidthValue;
    }

    public int getDelay(TopologyEdge topologyEdge) {
        return delayValue;
    }


}
