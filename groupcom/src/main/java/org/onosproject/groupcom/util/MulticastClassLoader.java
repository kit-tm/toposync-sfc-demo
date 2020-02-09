package org.onosproject.groupcom.util;

import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.flow.ITreeFlowPusher;
import thesiscode.common.group.AbstractGroupManager;
import thesiscode.common.tree.AbstractTreeAlgorithm;

import java.lang.reflect.InvocationTargetException;

/**
 * An utility class for loading the classes required for the multicast system.
 */
public class MulticastClassLoader {
    static Logger log = LoggerFactory.getLogger(MulticastClassLoader.class);

    /**
     * Loads the requested {@link AbstractGroupManager}.
     *
     * @param groupManagerFqName the fully qualified name of the group manager to load
     * @return an instance of the requested group manager, or null if the class could not be loaded
     */
    public static AbstractGroupManager getGroupManager(String groupManagerFqName, HostService hostService) {
        try {
            return (AbstractGroupManager) Class
                    .forName(groupManagerFqName)
                    .getConstructor(HostService.class)
                    .newInstance(hostService);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException
                | NoSuchMethodException e) {
            log.error(e.getMessage());
        }
        return null;
    }


    /**
     * Loads the requested {@link thesiscode.common.tree.AbstractTreeAlgorithm}.
     *
     * @param treeAlgoFqName the fully qualified name of the tree algo to load
     * @param topoService    the {@link TopologyService} to supply to the constructor of the
     *                       {@link thesiscode.common.tree.AbstractTreeAlgorithm} to
     *                       load.
     * @return an instance of the requested tree algorithm, or null if the class could not be loaded
     */
    public static AbstractTreeAlgorithm getTreeAlgo(String treeAlgoFqName, TopologyService topoService) {
        try {
            return (AbstractTreeAlgorithm) Class
                    .forName(treeAlgoFqName)
                    .getConstructor(TopologyService.class)
                    .newInstance(topoService);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException
                | ClassNotFoundException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * Loads the requested {@link ITreeFlowPusher}.
     *
     * @param flowPusherFQName the fully qualified name of the flow pusher to load
     * @param flowRuleService  the {@link FlowRuleService} to supply to the constructor of the {@link ITreeFlowPusher}
     *                         to load.
     * @return an instance of the requested flow pusher, or null if the class could not be loaded
     */
    public static ITreeFlowPusher getFlowPusher(String flowPusherFQName, FlowRuleService flowRuleService) {
        try {
            return (ITreeFlowPusher) Class
                    .forName(flowPusherFQName)
                    .getConstructor(FlowRuleService.class)
                    .newInstance(flowRuleService);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException
                | ClassNotFoundException e) {
            log.error(e.getMessage());
        }
        return null;
    }

}
