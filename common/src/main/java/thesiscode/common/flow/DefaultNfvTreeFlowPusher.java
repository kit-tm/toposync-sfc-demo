package thesiscode.common.flow;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.group.IGroupMember;

import java.util.Objects;
import java.util.Set;

public class DefaultNfvTreeFlowPusher extends NfvTreeFlowPusher {
    private Logger log = LoggerFactory.getLogger(getClass());


    public DefaultNfvTreeFlowPusher(ApplicationId appId, FlowRuleService flowRuleService) {
        super(appId, flowRuleService);
        this.appId = appId;
        this.flowRuleService = Objects.requireNonNull(flowRuleService);
    }

    @Override
    protected void installFlows(DeviceId device, int logicalEdge, ConnectPoint inCp, Set<ConnectPoint> outCps) {
        IGroupMember receiverAtSwitch = null;
        for (IGroupMember receiver : tree.getReceivers()) {
            if (receiver.getConnectPoint().deviceId().equals(device)) {
                receiverAtSwitch = receiver;
            }
        }

        firstRule(logicalEdge, device, receiverAtSwitch, inCp, outCps);

        if (receiverAtSwitch != null) {
            secondRule(device, receiverAtSwitch);
            thirdRule(device, outCps);
        }
    }

    private void firstRule(int logicalEdge, DeviceId currentSwitch, IGroupMember receiver, ConnectPoint inCp,
                           Set<ConnectPoint> outCps) {
        final boolean isSourceSwitch = currentSwitch.equals(tree.getSource().getConnectPoint().deviceId());

        TrafficSelector.Builder selBuild = DefaultTrafficSelector.builder(tree.getSelector()).matchInPort(inCp.port());
        if (!isSourceSwitch || (logicalEdge != 0)) {
            String macString = macString(logicalEdge);
            selBuild.matchEthDst(MacAddress.valueOf(macString));
        }
        TrafficSelector sel = selBuild.build();


        TrafficTreatment.Builder treatBuild = DefaultTrafficTreatment.builder();
        if (isSourceSwitch && (logicalEdge == 0)) {
            treatBuild.setEthDst(MacAddress.valueOf("11:11:11:11:11:11"));
        }
        if (receiver != null) {
            treatBuild.transition(1).deferred().setEthDst(receiver.getMacAddress());
        } else {
            for (ConnectPoint cp : outCps) {
                treatBuild.setOutput(cp.port());
            }
        }
        TrafficTreatment treat = treatBuild.build();

        install(sel, treat, currentSwitch, 0);
    }

    private void secondRule(DeviceId currentSwitch, IGroupMember receiver) {
        TrafficSelector sel = DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_IPV4).build();

        TrafficTreatment treat = DefaultTrafficTreatment.builder()
                                                        .transition(2)
                                                        .deferred()
                                                        .setIpDst(receiver.getIpAddress())
                                                        .build();

        install(sel, treat, currentSwitch, 1);
    }

    private void thirdRule(DeviceId device, Set<ConnectPoint> outCps) {
        TrafficTreatment.Builder treatBuild = DefaultTrafficTreatment.builder().deferred();
        for (ConnectPoint cp : outCps) {
            treatBuild.setOutput(cp.port());
        }
        TrafficTreatment treat = treatBuild.build();

        TrafficSelector sel = DefaultTrafficSelector.builder().matchEthType(Ethernet.TYPE_IPV4).build();

        install(sel, treat, device, 2);
    }

    /**
     * Calculates the MAC to use to mark the logical edge in packets ("tagging" to allow loops)
     *
     * @param logicalEdge the logical edge to compute the mac string for
     * @return the mac string
     */
    private String macString(int logicalEdge) {
        String macString = "";
        if (logicalEdge == 0) {
            macString = "11:11:11:11:11:11";
        } else if (logicalEdge == 1) {
            macString = "22:22:22:22:22:22";
        } else if (logicalEdge == 2) {
            macString = "33:33:33:33:33:33";
        }
        return macString;
    }


}
