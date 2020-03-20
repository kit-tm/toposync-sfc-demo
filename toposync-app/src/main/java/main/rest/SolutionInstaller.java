package main.rest;

import main.ClientServerLocator;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.flow.INfvTreeFlowPusher;
import thesiscode.common.group.AbstractMulticastGroup;
import thesiscode.common.group.IGroupMember;
import thesiscode.common.group.WrappedHost;
import thesiscode.common.group.igmp.IgmpGroupIdentifier;
import thesiscode.common.group.igmp.IgmpMulticastGroup;
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
    private NfvPlacementSolution solution;
    private INfvTreeFlowPusher flowPusher;
    private NfvInstantiator instantiator;

    private DeviceService deviceService;
    private HostService hostService;

    public SolutionInstaller(INfvTreeFlowPusher flowPusher, NfvInstantiator instantiator, DeviceService deviceService
            , HostService hostService) {
        this.flowPusher = flowPusher;
        this.instantiator = instantiator;
        this.deviceService = deviceService;
        this.hostService = hostService;
    }

    public void installSolution(NfvPlacementSolution solution) {
        uninstallOldSolution();
        this.solution = solution;
        Map<NprNfvTypes.Type, Set<ConnectPoint>> vnfConnectPoints = placeVNFs();
        pushFlows(vnfConnectPoints);
    }

    private void uninstallOldSolution() {
        if (solution != null) {
            flowPusher.deleteFlows();
            // TODO remove VNFs
        }
    }

    private Map<NprNfvTypes.Type, Set<ConnectPoint>> placeVNFs() {
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
}
