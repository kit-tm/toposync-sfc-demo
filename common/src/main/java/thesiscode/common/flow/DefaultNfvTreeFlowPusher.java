package thesiscode.common.flow;

import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.flow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.group.IGroupMember;
import thesiscode.common.tree.NFVPerSourceTree;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultNfvTreeFlowPusher extends DefaultPerSourceTreeFlowPusher implements INfvTreeFlowPusher {
    private Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    private FlowRuleService flowRuleService;

    private NFVPerSourceTree tree;
    private List<FlowRule> installed = new ArrayList<>();


    public DefaultNfvTreeFlowPusher(ApplicationId appId, FlowRuleService flowRuleService) {
        super(appId, flowRuleService);
        this.appId = appId;
        this.flowRuleService = Objects.requireNonNull(flowRuleService);
    }


    @Override
    public void deleteFlows() {
        super.deleteFlows();
        flowRuleService.removeFlowRules(installed.toArray(new FlowRule[0]));
    }

    @Override
    public void pushTree(NFVPerSourceTree tree) {
        this.tree = Objects.requireNonNull(tree);
        log.info("pushing tree {}", tree);
        log.info("tree.getSource {}", tree.getSource());
        log.info("tree.getLinks {}", tree.getLinks());
        log.info("tree.getReceivers {}", tree.getReceivers());
        log.info("tree.getVnfCps {}", tree.getVnfConnectPoints());


        for (int logicalEdge = 0; logicalEdge < tree.getLinks().size(); logicalEdge++) {
            Set<Link> linkForLogical = tree.getLinks().get(logicalEdge);
            Map<DeviceId, Set<Link>> outLinksPerDevice = groupByOutgoing(linkForLogical);
            Map<DeviceId, Set<Link>> inLinksPerDevice = groupByIngoing(linkForLogical);

            Set<DeviceId> allInvolvedDevices = new HashSet<>();
            allInvolvedDevices.addAll(inLinksPerDevice.keySet());
            allInvolvedDevices.addAll(outLinksPerDevice.keySet());

            for (DeviceId device : allInvolvedDevices) {
                ConnectPoint inCp = inCp(device, inLinksPerDevice.get(device), logicalEdge);
                TrafficSelector sel = match(device, inCp, logicalEdge);

                Set<ConnectPoint> outCps = outCps(device, outLinksPerDevice.get(device), logicalEdge);
                TrafficTreatment treat = action(device, outCps);

                FlowRule fr = DefaultFlowRule.builder()
                                             .forDevice(device)
                                             .withSelector(sel)
                                             .withTreatment(treat)
                                             .makePermanent()
                                             .withPriority(FlowRule.MAX_PRIORITY)
                                             .forTable(0)
                                             .fromApp(appId)
                                             .build();

                log.info("adding VNF flow rule: {}", fr);
                flowRuleService.applyFlowRules(fr);
                installed.add(fr);
            }
        }
    }

    private TrafficSelector match(DeviceId currentSwitch, ConnectPoint inCp, int logicalEdge) {
        TrafficSelector.Builder selBuild = DefaultTrafficSelector.builder(tree.getSelector()).matchInPort(inCp.port());

        if (!currentSwitch.equals(tree.getSource().getConnectPoint().deviceId())) {
            String macString = macString(logicalEdge);
            selBuild.matchEthDst(MacAddress.valueOf(macString));
        }
        return selBuild.build();
    }

    private TrafficTreatment action(DeviceId currentSwitch, Set<ConnectPoint> outCps) {
        TrafficTreatment.Builder treatBuild = DefaultTrafficTreatment.builder();

        final boolean isSourceSwitch = currentSwitch.equals(tree.getSource().getConnectPoint().deviceId());

        if (isSourceSwitch) {
            treatBuild.setEthDst(MacAddress.valueOf("11:11:11:11:11:11"));
        }

        for (IGroupMember receiver : tree.getReceivers()) {
            if (receiver.getConnectPoint().deviceId().equals(currentSwitch)) {
                treatBuild.setEthDst(receiver.getMacAddress());
                treatBuild.setIpDst(receiver.getIpAddress());
            }
        }

        for (ConnectPoint cp : outCps) {
            treatBuild.setOutput(cp.port());
        }

        return treatBuild.build();
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

    /**
     * Calculates the ingoing connectpoint used for the flow rule to be installed.
     *
     * @param currentSwitch the switch for which the flow rule is to be computed
     * @param inLinks       the ingoing links of this switch
     * @param logicalEdge   the logical edge (overlay edge) of the tree
     * @return the connect point to use as in match
     */
    private ConnectPoint inCp(DeviceId currentSwitch, Set<Link> inLinks, int logicalEdge) {
        final boolean isSourceSwitch = currentSwitch.equals(tree.getSource().getConnectPoint().deviceId());

        ConnectPoint inCp;

        if (isSourceSwitch) {
            // first switch (where source host is attached to)
            inCp = tree.getSource().getConnectPoint();
        } else {
            if (inLinks == null) {
                // was null because device is VNF source
                log.info("current device is VNF source: {}", currentSwitch.toString());
                inCp = tree.getVnfConnectPoints().get(logicalEdge - 1).iterator().next();
            } else {
                log.info("current device is intermediate: {}", currentSwitch.toString());
                inCp = inLinks.iterator().next().dst();
            }
        }
        log.info("inCP: {}", inCp);
        return inCp;
    }

    /**
     * Calculates the outgoing connectpoint used for the flow rule to be installed.
     *
     * @param currentSwitch the switch where the flow rule is to be installed
     * @param outLinks      the outgoing links of this switch
     * @param logicalEdge   the logical edge
     * @return the outgoing connect points
     */
    private Set<ConnectPoint> outCps(DeviceId currentSwitch, Set<Link> outLinks, int logicalEdge) {
        log.info("calculating outCps for {}", currentSwitch);
        Set<ConnectPoint> outCps = new HashSet<>();
        if (logicalEdge < tree.getVnfConnectPoints().size()) {
            for (ConnectPoint vnfCp : tree.getVnfConnectPoints().get(logicalEdge)) {
                if (vnfCp.deviceId().equals(currentSwitch)) {
                    // at current device, VNF is attached
                    outCps = new HashSet<>();
                    outCps.add(vnfCp);
                }
            }
        }

        log.info("outLinks: {}", outLinks);
        if (outLinks != null) {
            outCps.addAll(outLinks.stream().map(Link::src).collect(Collectors.toSet()));
        }

        if (logicalEdge == (tree.getLinks().size() - 1)) {
            for (IGroupMember dst : tree.getReceivers()) {
                log.info("dst: {}", dst.getIpAddress());
                log.info("dst.cp.devId: {}", dst.getConnectPoint().deviceId());
                if (currentSwitch.equals(dst.getConnectPoint().deviceId())) {
                    outCps.add(dst.getConnectPoint());
                }
            }
        }

        log.info("outCPs: {}", outCps);
        return outCps;
    }

    private Map<DeviceId, Set<Link>> groupByOutgoing(Set<Link> links) {
        Map<DeviceId, Set<Link>> outgoing = new HashMap<>();
        for (Link link : links) {
            outgoing.computeIfAbsent(link.src().deviceId(), k -> new HashSet<>());
            outgoing.get(link.src().deviceId()).add(link);
        }
        return outgoing;
    }

    private Map<DeviceId, Set<Link>> groupByIngoing(Set<Link> links) {
        Map<DeviceId, Set<Link>> ingoing = new HashMap<>();
        for (Link link : links) {
            ingoing.computeIfAbsent(link.dst().deviceId(), k -> new HashSet<>());
            ingoing.get(link.dst().deviceId()).add(link);
        }
        return ingoing;
    }

}
