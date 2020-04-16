package main.rest.provide;

import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;

/**
 * Invalidates the last solution that is provided via a {@link TreeProvider} when the topology changed.
 */
public class SolutionInvalidator implements TopologyListener {
    private final TreeProvider treeProvider;
    private int lastDeviceCount;

    public SolutionInvalidator(TreeProvider treeProvider) {
        this.treeProvider = treeProvider;
    }

    @Override
    public void event(TopologyEvent topologyEvent) {
        final int newDeviceCount = topologyEvent.subject().deviceCount();
        final boolean deviceCountChanged = (newDeviceCount != lastDeviceCount);

        if (deviceCountChanged) {
            treeProvider.setLastSolution(null);
            this.lastDeviceCount = newDeviceCount;
        }
    }
}
