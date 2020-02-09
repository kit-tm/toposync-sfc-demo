package thesiscode.common.flow;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import thesiscode.common.tree.ISharedTree;
import thesiscode.common.tree.TreeChangeEvent;

import java.util.List;

// TODO
public class DefaultSharedTreeFlowPusher implements ISharedTreeFlowPusher {
    private ApplicationId appId;
    private FlowRuleService flowRuleService;

    private List<FlowRule> installed;


    public DefaultSharedTreeFlowPusher(ApplicationId appId, FlowRuleService flowRuleService) {
        this.appId = appId;
        this.flowRuleService = flowRuleService;
    }

    @Override
    public void pushTree(ISharedTree tree) {

    }

    @Override
    public void update(TreeChangeEvent treeChangeEvent) {

    }

    @Override
    public void deleteFlows() {
        for (FlowRule fr : installed) {
            flowRuleService.removeFlowRules(fr);
        }
    }
}
