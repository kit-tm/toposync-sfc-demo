package main.rest;

import main.rest.provide.TreeProvider;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;

/**
 * Invalidates the last solution that is provided via a {@link TreeProvider} and stored in
 * {@link SolutionInstaller} when the topology changed.
 */
public class SolutionInvalidator implements TopologyListener {
    private final TreeProvider treeProvider;
    private final SolutionInstaller installer;
    private int lastDeviceCount;

    public SolutionInvalidator(TreeProvider treeProvider, SolutionInstaller installer) {
        this.treeProvider = treeProvider;
        this.installer = installer;
    }

    @Override
    public void event(TopologyEvent topologyEvent) {
        final int newDeviceCount = topologyEvent.subject().deviceCount();
        final boolean deviceCountChanged = (newDeviceCount != lastDeviceCount);

        if (deviceCountChanged) {
            treeProvider.setLastSolution(null);
            installer.invalidateSolution();
            this.lastDeviceCount = newDeviceCount;
        }
    }
}
