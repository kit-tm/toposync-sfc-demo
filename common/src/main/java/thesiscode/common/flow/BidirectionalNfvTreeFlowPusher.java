package thesiscode.common.flow;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.*;
import thesiscode.common.tree.NFVPerSourceTree;

import java.util.Objects;
import java.util.Set;

public class BidirectionalNfvTreeFlowPusher extends NfvTreeFlowPusher {
    private DefaultNfvTreeFlowPusher forwardPusher;

    public BidirectionalNfvTreeFlowPusher(ApplicationId appId, FlowRuleService flowRuleService) {
        super(appId, flowRuleService);
        this.forwardPusher = new DefaultNfvTreeFlowPusher(appId, flowRuleService);
    }

    @Override
    public void pushTree(NFVPerSourceTree tree) {
        this.tree = Objects.requireNonNull(tree);
        forwardPusher.pushTree(tree); // push forwards
        super.pushTree(tree); // push backwards (calls installFlows() as template method)
    }

    @Override
    protected void installFlows(DeviceId device, int logicalEdge, ConnectPoint inCp, Set<ConnectPoint> outCps) {
        TrafficTreatment treat = DefaultTrafficTreatment.builder().setOutput(inCp.port()).build();

        for (ConnectPoint outCp : outCps) {
            TrafficSelector sel = DefaultTrafficSelector.builder()
                                                        .matchInPort(outCp.port())
                                                        .matchEthType(Ethernet.TYPE_IPV4)
                                                        .matchIPDst(IpAddress.valueOf("10.0.0.1").toIpPrefix())
                                                        .build();

            install(sel, treat, device, 0);
        }
    }
}
