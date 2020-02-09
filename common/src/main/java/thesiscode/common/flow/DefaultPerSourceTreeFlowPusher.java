package thesiscode.common.flow;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.tree.IPerSourceTree;
import thesiscode.common.tree.TreeChangeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DefaultPerSourceTreeFlowPusher implements IPerSourceTreeFlowPusher {
    private Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    private FlowRuleService flowRuleService;

    private List<FlowRule> installed = new ArrayList<>();


    public DefaultPerSourceTreeFlowPusher(ApplicationId appId, FlowRuleService flowRuleService) {
        this.appId = appId;
        this.flowRuleService = Objects.requireNonNull(flowRuleService);
    }


    @Override
    public void pushTree(IPerSourceTree tree) {
        Set<ConnectPoint> edgeConnectPoints = new HashSet<>();
        edgeConnectPoints.add(tree.getSource().getConnectPoint());
        tree.getReceivers().forEach(r -> {
            log.info("adding receiver edge cp: {}", r.getConnectPoint());
            edgeConnectPoints.add(r.getConnectPoint());
        });
        log.info("edgeConnectPoints: {}", edgeConnectPoints);

        Set<Link> links = tree.getLinks();
        log.info("tree to push: {}", links);


        // Set<Link> -> Map<DeviceId,List<PortNumber>>
        Map<DeviceId, List<PortNumber>> portsBySourceDeviceId = linksToMap(links, edgeConnectPoints);
        log.debug("portsBySourceDeviceId: {}", portsBySourceDeviceId);

        // traffic selector = match
        TrafficSelector treeSelector = tree.getSelector();


        // build flow rules
        List<FlowRule> flowRules = new ArrayList<>();


        for (DeviceId deviceId : portsBySourceDeviceId.keySet()) {

            // out ports on tree
            List<PortNumber> outPorts = portsBySourceDeviceId.get(deviceId);

            log.info("final out ports for {}: {}", deviceId, outPorts);
            // traffic treatment = actions
            TrafficTreatment treatment = buildTrafficTreatment(outPorts);


            // compose selector and treatment to flow rule
            FlowRule fr = DefaultFlowRule.builder()
                                         .forDevice(deviceId)
                                         .fromApp(appId)
                                         .withPriority(FlowRule.MAX_PRIORITY)
                                         .withSelector(treeSelector)
                                         .withTreatment(treatment)
                                         .makePermanent()
                                         .forTable(0)
                                         .build();

            flowRules.add(fr);
        }

        log.debug("all flow rules: {}.", flowRules);
        log.debug("now applying flow rules.");

        // install flow rules
        flowRuleService.applyFlowRules(flowRules.toArray(new FlowRule[0]));

        installed.addAll(flowRules);
    }

    @Override
    public void update(TreeChangeEvent treeChangeEvent) {
        throw new IllegalStateException("update() not yet implemented");
    }

    private Map<DeviceId, List<PortNumber>> linksToMap(Iterable<Link> links, Set<ConnectPoint> edgeCps) {
        Map<DeviceId, List<PortNumber>> portsByDeviceId = new HashMap<>();
        for (Link curLink : links) {
            DeviceId srcDeviceId = curLink.src().deviceId();
            PortNumber out = curLink.src().port();
            log.debug("#looking at link {}", curLink);
            log.debug("deviceid: {}, port: {}", srcDeviceId, out);


            List<PortNumber> portList = portsByDeviceId.get(srcDeviceId);
            if (portList == null) {
                List<PortNumber> newPortList = new ArrayList<>();
                newPortList.add(out);
                portsByDeviceId.put(srcDeviceId, newPortList);
                log.debug("added because null before");
            } else {
                if (!portList.contains(out)) {
                    portList.add(out);
                    log.debug("added because existing before and not yet contained");
                }

            }

            log.debug("#finished link {}, new portsByDeviceId: {}", curLink, portsByDeviceId);
        }

        for (ConnectPoint edgeCp : edgeCps) {
            PortNumber edgeOut = edgeCp.port();
            DeviceId edgeDeviceId = edgeCp.deviceId();
            log.info("edge-port {} to {}", edgeOut, edgeDeviceId);
            List<PortNumber> edgePortList = portsByDeviceId.get(edgeDeviceId);
            if (edgePortList == null) {
                List<PortNumber> newPortList = new ArrayList<>();
                newPortList.add(edgeOut);
                portsByDeviceId.put(edgeDeviceId, newPortList);
            } else {
                if (!edgePortList.contains(edgeOut)) {
                    edgePortList.add(edgeOut);
                }
            }
        }

        return portsByDeviceId;
    }


    private TrafficTreatment buildTrafficTreatment(List<PortNumber> portNumbers) {
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        for (PortNumber portNumber : portNumbers) {
            treatmentBuilder.setOutput(portNumber);
        }

        return treatmentBuilder.build();
    }

    @Override
    public void deleteFlows() {
        installed.forEach(flowRuleService::removeFlowRules);
    }
}
