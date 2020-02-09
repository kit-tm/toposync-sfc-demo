package thesiscode.common.flow;

import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Link;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.group.WrappedHost;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PathFlowPusher implements IPathFlowPusher {
    private Logger log = LoggerFactory.getLogger(getClass());

    private List<FlowRule> installedFlowRules = new ArrayList<>();
    private FlowRuleService flowRuleService;
    private ApplicationId appId;

    private byte[] ipProtos;

    public PathFlowPusher(ApplicationId appId, FlowRuleService flowRuleService, byte[] ipProtosToRedirect) {
        this.appId = appId;
        this.flowRuleService = flowRuleService;
        this.ipProtos = ipProtosToRedirect;
    }

    @Override
    public void pushPath(WrappedHost source, Set<Link> path, WrappedHost destination) {
        List<FlowRule> flowRuleList = new ArrayList<>();

        // infrastructure link flows
        for (Link link : path) {
            flowRuleList.addAll(getFlowsForLink(source, link));
        }

        // last hop
        flowRuleList.addAll(getFlowsForLastHop(source, destination));

        log.info("final flow rules: {}", flowRuleList);

        flowRuleService.applyFlowRules(flowRuleList.toArray(new FlowRule[0]));
        installedFlowRules.addAll(flowRuleList);
    }

    private Set<FlowRule> getFlowsForLink(WrappedHost source, Link link) {
        Set<FlowRule> flowRules = new HashSet<>();

        // path = consecutive chain of links -> always one output
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(link.src().port()).build();

        for (byte proto : ipProtos) {
            TrafficSelector selector = getSelector(source, proto);

            flowRules.add(DefaultFlowRule
                                  .builder()
                                  .forDevice(link.src().deviceId())
                                  .withTreatment(treatment)
                                  .withSelector(selector)
                                  .makePermanent()
                                  .forTable(0)
                                  .withPriority(FlowRule.MAX_PRIORITY)
                                  .fromApp(appId)
                                  .build());
        }

        return flowRules;
    }

    private Set<FlowRule> getFlowsForLastHop(WrappedHost source, WrappedHost destination) {
        Set<FlowRule> flowRules = new HashSet<>();

        // treatment
        TrafficTreatment treatment = DefaultTrafficTreatment
                .builder()
                .setOutput(destination.getConnectPoint().port())
                .build();

        for (byte proto : ipProtos) {
            TrafficSelector selector = getSelector(source, proto);


            flowRules.add(DefaultFlowRule
                                  .builder()
                                  .forDevice(destination.getConnectPoint().deviceId())
                                  .withTreatment(treatment)
                                  .withSelector(selector)
                                  .makePermanent()
                                  .forTable(0)
                                  .withPriority(FlowRule.MAX_PRIORITY)
                                  .fromApp(appId)
                                  .build());
        }

        return flowRules;
    }

    private TrafficSelector getSelector(WrappedHost source, byte ipProto) {
        Set<Criterion> matchCriteria = new HashSet<>(source.getMatchCriterionSet(true));
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        for (Criterion criterion : matchCriteria) {
            selectorBuilder.add(criterion);
        }
        selectorBuilder.matchIPProtocol(ipProto);
        selectorBuilder.matchIPDst(IpPrefix.IPV4_MULTICAST_PREFIX);
        return selectorBuilder.build();
    }

    @Override
    public void deleteFlows() {
        flowRuleService.removeFlowRules(installedFlowRules.toArray(new FlowRule[0]));
    }
}
