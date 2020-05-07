package main.rest;

import main.ClientServerLocator;
import main.ProgressMonitor;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.flow.INfvTreeFlowPusher;
import thesiscode.common.group.AbstractMulticastGroup;
import thesiscode.common.group.IGroupMember;
import thesiscode.common.group.WrappedHost;
import thesiscode.common.group.igmp.IgmpGroupIdentifier;
import thesiscode.common.group.igmp.IgmpMulticastGroup;
import thesiscode.common.nfv.placement.deploy.InstantiationException;
import thesiscode.common.nfv.placement.deploy.NfvInstantiator;
import thesiscode.common.nfv.placement.solver.NfvPlacementSolution;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.WrappedPoPVertex;
import thesiscode.common.tree.NFVPerSourceTree;

import java.util.*;
import java.util.stream.Collectors;

public class SolutionInstaller {
    public static final String GROUP_IP = "224.2.3.4";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private NfvPlacementSolution solution;
    private INfvTreeFlowPusher flowPusher;
    private NfvInstantiator instantiator;

    private ProgressMonitor progressMonitor;

    private DeviceService deviceService;
    private HostService hostService;

    public NfvPlacementSolution getInstalledSolution() {
        return solution;
    }

    public void invalidateSolution() {
        this.solution = null;
    }

    public SolutionInstaller(INfvTreeFlowPusher flowPusher, NfvInstantiator instantiator, DeviceService deviceService
            , HostService hostService, ProgressMonitor progressMonitor) {
        this.flowPusher = flowPusher;
        this.instantiator = instantiator;
        this.deviceService = deviceService;
        this.hostService = hostService;
        this.progressMonitor = progressMonitor;
    }

    public void installSolution(NfvPlacementSolution solution) throws InstantiationException {
        log.info("uninstalling old solution...");
        uninstallOldSolution();
        this.solution = solution;
        log.info("placing VNFs..");
        Map<NprNfvTypes.Type, Set<ConnectPoint>> vnfConnectPoints = placeVNFs();
        progressMonitor.vnfPlaced();
        log.info("pushing flows..");
        pushFlows(vnfConnectPoints);
        progressMonitor.flowsInstalled();
    }

    protected void uninstallOldSolution() throws InstantiationException {
        if (solution != null) {
            log.info("deleting old flow rules");
            flowPusher.deleteFlows();
            log.info("removing old VNF instances");
            removeVNFs();
            progressMonitor.oldSolutionUninstalled();
        }
    }

    private void removeVNFs() throws InstantiationException {
        for (Map.Entry<NprNfvTypes.Type, Set<TopologyVertex>> entry : solution.getSharedPlacements().entrySet()) {
            NprNfvTypes.Type vnfType = entry.getKey();
            Set<TopologyVertex> placementSwitches = entry.getValue();

            for (TopologyVertex placementSwitch : placementSwitches) {
                boolean hwAccelerated = ((WrappedPoPVertex) placementSwitch).hwAccelerationOffered(vnfType);
                instantiator.remove(vnfType, deviceService.getDevice(placementSwitch.deviceId()), hwAccelerated);
            }
        }
    }

    private Map<NprNfvTypes.Type, Set<ConnectPoint>> placeVNFs() throws InstantiationException {
        Map<NprNfvTypes.Type, Set<TopologyVertex>> toInstantiate = solution.getSharedPlacements();
        Map<NprNfvTypes.Type, Set<ConnectPoint>> vnfCps = new HashMap<>();

        for (NprNfvTypes.Type type : toInstantiate.keySet()) {
            Set<ConnectPoint> cpsForType = new HashSet<>();
            vnfCps.put(type, cpsForType);
            for (TopologyVertex vert : toInstantiate.get(type)) {
                if (vert instanceof WrappedPoPVertex) {
                    WrappedPoPVertex wrapped = (WrappedPoPVertex) vert;
                    if (wrapped.hwAccelerationOffered(type)) {
                        cpsForType.add(instantiator.instantiate(type, deviceService.getDevice(vert.deviceId()), true));
                    } else {
                        cpsForType.add(instantiator.instantiate(type, deviceService.getDevice(vert.deviceId()), false));
                    }
                } else {
                    throw new IllegalStateException("Trying to instantiate VNF at vert which is not a PoP (" + type.name() + "@" + vert
                            .deviceId()
                            .toString() + ")");
                }
            }
        }
        return vnfCps;
    }

    private void pushFlows(Map<NprNfvTypes.Type, Set<ConnectPoint>> vnfCps) {
        IGroupMember src = new WrappedHost(hostService.getHostsByIp(ClientServerLocator.SERVER_ADDRESS)
                                                      .iterator()
                                                      .next());
        Set<IGroupMember> dsts = new HashSet<>();
        dsts.add(new WrappedHost(hostService.getHostsByIp(ClientServerLocator.CLIENT_1_ADDRESS).iterator().next()));
        dsts.add(new WrappedHost(hostService.getHostsByIp(ClientServerLocator.CLIENT_2_ADDRESS).iterator().next()));

        final NprTraffic traffic = solution.getRequest().getTraffic().get(0);

        List<Set<TopologyEdge>> solutionEdges = solution.getLogicalEdgesPerTraffic().get(traffic);
        if (!(solutionEdges.size() == traffic.getSfc().size() + 1)) {
            throw new IllegalStateException("wrong solution edges size: " + solutionEdges.size() + ", " + "expected " + (traffic.getSfc()
                                                                                                                                .size() + 1));
        }

        List<Set<Link>> solutionLinks = solutionEdges.stream()
                                                     .map(x -> x.stream()
                                                                .map(TopologyEdge::link)
                                                                .collect(Collectors.toSet()))
                                                     .collect(Collectors.toList());

        List<Set<ConnectPoint>> solutionVnfCps = new ArrayList<>();
        for (NprNfvTypes.Type type : traffic.getSfc()) {
            solutionVnfCps.add(vnfCps.get(type));
        }

        AbstractMulticastGroup group = new IgmpMulticastGroup(new IgmpGroupIdentifier(Ip4Address.valueOf(GROUP_IP)));
        NFVPerSourceTree nfvTree = new NFVPerSourceTree(src, solutionLinks, dsts, solutionVnfCps, group);
        flowPusher.pushTree(nfvTree);
    }

    public void setProgressMonitor(ProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    public ProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }
}
