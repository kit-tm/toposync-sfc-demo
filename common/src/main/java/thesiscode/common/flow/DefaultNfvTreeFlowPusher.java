package thesiscode.common.flow;

import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.group.IGroupMember;
import thesiscode.common.tree.NFVPerSourceTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultNfvTreeFlowPusher extends DefaultPerSourceTreeFlowPusher implements INfvTreeFlowPusher {
    private Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    private FlowRuleService flowRuleService;

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
        log.info("pushing tree {}", tree);
        log.info("tree.getSource {}", tree.getSource());
        log.info("tree.getLinks {}", tree.getLinks());
        log.info("tree.getReceivers {}", tree.getReceivers());
        log.info("tree.getVnfCps {}", tree.getVnfConnectPoints());


        for (int logicalEdge = 0; logicalEdge < tree.getLinks().size(); logicalEdge++) {
            Set<Link> linkForLogical = tree.getLinks().get(logicalEdge);
            Map<DeviceId, Set<Link>> outLinksPerDevice = getOutgoingLinksForDevices(linkForLogical);
            Map<DeviceId, Set<Link>> inLinksPerDevice = getIngoingLinksForDevices(linkForLogical);
            Set<DeviceId> allInvolvedDevices = new HashSet<>();
            allInvolvedDevices.addAll(inLinksPerDevice.keySet());
            allInvolvedDevices.addAll(outLinksPerDevice.keySet());

            for (DeviceId device : allInvolvedDevices) {
                ConnectPoint inCp;
                Set<ConnectPoint> outCps = new HashSet<>();

                if (device.equals(tree.getSource().getConnectPoint().deviceId())) {
                    // first switch (where source host is attached to)
                    inCp = tree.getSource().getConnectPoint();
                } else {
                    if (inLinksPerDevice.get(device) == null) {
                        // was null because device is VNF source
                        log.info("current device is VNF source: {}", device.toString());
                        inCp = tree.getVnfConnectPoints().get(logicalEdge - 1).iterator().next();
                    } else {
                        log.info("current device is intermediate: {}", device.toString());
                        inCp = inLinksPerDevice.get(device).iterator().next().dst();
                    }
                }

                log.info("inCP: {}", inCp);

                if (logicalEdge < tree.getVnfConnectPoints().size()) {
                    for (ConnectPoint vnfCp : tree.getVnfConnectPoints().get(logicalEdge)) {
                        if (vnfCp.deviceId().equals(device)) {
                            // at current device, VNF is attached
                            outCps = new HashSet<>();
                            outCps.add(vnfCp);
                        }
                    }
                }

                if (outLinksPerDevice.get(device) != null) {
                    outCps.addAll(outLinksPerDevice.get(device).stream().map(Link::src).collect(Collectors.toSet()));
                }

                if (logicalEdge == (tree.getLinks().size() - 1)) {
                    for (IGroupMember dst : tree.getReceivers()) {
                        if (device.equals(dst.getConnectPoint().deviceId())) {
                            outCps.add(dst.getConnectPoint());
                        }
                    }
                }

                log.info("outCPs: {}", outCps);

                String macString = "";
                if (logicalEdge == 0) {
                    macString = "11:11:11:11:11:11";
                } else if (logicalEdge == 1) {
                    macString = "22:22:22:22:22:22";
                } else if (logicalEdge == 2) {
                    macString = "33:33:33:33:33:33";
                }

                TrafficSelector.Builder selBuild = DefaultTrafficSelector.builder(tree.getSelector())
                                                                         .matchInPort(inCp.port());

                if (!device.equals(tree.getSource().getConnectPoint().deviceId())) {
                    selBuild.matchEthDst(MacAddress.valueOf(macString));
                }

                TrafficSelector sel = selBuild.build();

                TrafficTreatment.Builder treatBuild = DefaultTrafficTreatment.builder();

                if (device.equals(tree.getSource().getConnectPoint().deviceId())) {
                    // if this is the first switch change eth dst to match logical links
                    treatBuild.setEthDst(MacAddress.valueOf("11:11:11:11:11:11"));
                }

                for (ConnectPoint cp : outCps) {
                    treatBuild.setOutput(cp.port());

                }


                FlowRule fr = DefaultFlowRule.builder()
                                             .forDevice(device)
                                             .withSelector(sel)
                                             .withTreatment(treatBuild.build())
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


        // divide links into a set of links whose source is the VNF switch and residual
        /*Set<DeviceId> vnfDeviceIds = tree.getVnfConnectPoints()
                                         .stream()
                                         .map(ConnectPoint::deviceId)
                                         .collect(Collectors.toSet());
        Set<Link> linksFromVnfSwitch = new HashSet<>();
        Set<Link> residualLinks = new HashSet<>();

        for (Link link : tree.getLinks()) {
            if (vnfDeviceIds.contains(link.src().deviceId())) {
                linksFromVnfSwitch.add(link);
            } else {
                residualLinks.add(link);
            }
        }

        // push "residual tree" like usual
        IPerSourceTree residualTree = new DefaultPerSourceTree(tree.getSource(), residualLinks, tree.getReceivers());
        log.info("start pushing residual tree");
        super.pushTree(residualTree);
        log.info("finished pushing residual tree");*/

        // push rules for VNF
        //pushVnfRules(tree, linksFromVnfSwitch);
    }

    private Map<DeviceId, Set<Link>> getOutgoingLinksForDevices(Set<Link> links) {
        Map<DeviceId, Set<Link>> outgoing = new HashMap<>();
        for (Link link : links) {
            outgoing.computeIfAbsent(link.src().deviceId(), k -> new HashSet<>());
            outgoing.get(link.src().deviceId()).add(link);
        }
        return outgoing;
    }

    private Map<DeviceId, Set<Link>> getIngoingLinksForDevices(Set<Link> links) {
        Map<DeviceId, Set<Link>> ingoing = new HashMap<>();
        for (Link link : links) {
            ingoing.computeIfAbsent(link.dst().deviceId(), k -> new HashSet<>());
            ingoing.get(link.dst().deviceId()).add(link);
        }
        return ingoing;
    }

    /*private void pushVnfRules(NFVPerSourceTree tree, Set<Link> linksFromVnfSwitch) {
        Set<ConnectPoint> vnfCps = tree.getVnfConnectPoints();
        log.info("vnfCps: {}, linksFromVnfSwitch: {}", vnfCps, linksFromVnfSwitch);

        Set<FlowRule> flowRules = new HashSet<>();
        for (ConnectPoint cp : vnfCps) {
            // redirect group traffic to VNF
            flowRules.add(DefaultFlowRule.builder()
                                         .forDevice(cp.deviceId())
                                         .fromApp(appId)
                                         .withPriority(FlowRule.MAX_PRIORITY - 1)
                                         .withSelector(tree.getSelector())
                                         .withTreatment(DefaultTrafficTreatment.builder()
                                                                               .setOutput(cp.port())
                                                                               .transition(1)
                                                                               .build())
                                         .makePermanent()
                                         .forTable(0)
                                         .build());

            // redirect processed traffic from VNF according to tree
            flowRules.add(DefaultFlowRule.builder()
                                         .forDevice(cp.deviceId())
                                         .fromApp(appId)
                                         .withPriority(FlowRule.MAX_PRIORITY)
                                         .withSelector(DefaultTrafficSelector.builder(tree.getSelector())
                                                                             .matchInPort(cp.port())
                                                                             .build())
                                         .withTreatment(getTreatmentFromVnfToTree(cp.deviceId(), linksFromVnfSwitch))
                                         .makePermanent()
                                         .forTable(0)
                                         .build());
        }


        for (FlowRule fr : flowRules) {
            installed.add(fr);
            log.info("adding VNF flow rule: {}", fr);
        }
        flowRuleService.applyFlowRules(flowRules.toArray(new FlowRule[0]));
    }*/

    /**
     * Filters the list of links so that the result only contains links whose src device id equals id. Then builds a
     * traffic treatment by adding the source port of each link as output port.
     *
     * @param id                 the id to filter the set for
     * @param linksFromVnfSwitch the set of links to filter (may contain links whose src is different from id, hence
     *                           the filtering)
     * @return a traffic treatment which contains each out port of each link whose src is id
     */
    private TrafficTreatment getTreatmentFromVnfToTree(DeviceId id, Set<Link> linksFromVnfSwitch) {
        Set<Link> linksFromId = linksFromVnfSwitch.stream()
                                                  .filter(l -> l.src().deviceId().equals(id))
                                                  .collect(Collectors.toSet());


        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();

        for (Link link : linksFromId) {
            builder.setOutput(link.src().port());
        }

        return builder.build();
    }

}
