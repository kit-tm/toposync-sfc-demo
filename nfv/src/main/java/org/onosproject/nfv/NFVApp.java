package org.onosproject.nfv;

import gurobi.GRBEnv;
import gurobi.GRBException;
import org.apache.felix.scr.annotations.*;
import org.onlab.packet.*;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.groupcom.util.GroupManagementService;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.*;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.*;
import org.onosproject.net.topology.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.flow.DefaultNfvTreeFlowPusher;
import thesiscode.common.flow.PathFlowPusher;
import thesiscode.common.group.*;
import thesiscode.common.group.igmp.IgmpGroupIdentifier;
import thesiscode.common.nfv.placement.deploy.NfvInstantiator;
import thesiscode.common.nfv.placement.solver.*;
import thesiscode.common.nfv.placement.solver.ilp32.Ilp32Solver;
import thesiscode.common.nfv.placement.solver.mfcp.used.RefSfcPlacementSolver;
import thesiscode.common.nfv.traffic.*;
import thesiscode.common.topo.*;
import thesiscode.common.tree.NFVPerSourceTree;

import java.util.*;
import java.util.stream.Collectors;

@Component(immediate = true)
public class NFVApp implements PacketProcessor, TopologyListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    public static ApplicationId appId;

    private Host nfvi;
    private PathFlowPusher pusher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PacketService pktService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private TopologyService topoService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private GroupManagementService groupManagementService;

    private INfvPlacementSolver solver;
    private NfvInstantiator instantiator;

    private boolean first = true;

    private GRBEnv env;

    @Activate
    public void activate() throws GRBException {
        log.info("installed!");
        appId = coreService.registerApplication("org.onosproject.nfv");
        byte[] ipProtosToRedirect = {IPv4.PROTOCOL_UDP};
        pusher = new PathFlowPusher(appId, flowRuleService, ipProtosToRedirect);
        pktService.addProcessor(this, PacketProcessor.director(3));

        installDefaultFlows();

        solver = new Ilp32Solver();
        instantiator = new NfvInstantiator();

        log.info("Creating GRB Environment.");
        env = new GRBEnv("/home/felix/from_app.log");
        log.info("Created GRB Environment: {}", env);
    }

    @Deactivate
    public void deactivate() throws GRBException {
        pktService.removeProcessor(this);
        pusher.deleteFlows();
        flowRuleService.removeFlowRulesById(appId);
        env.dispose();
    }

    @Override
    public void process(PacketContext packetContext) {
        InboundPacket in = packetContext.inPacket();
        ConnectPoint inCp = in.receivedFrom();
        Ethernet eth = in.parsed();
        if (eth.getEtherType() == Ethernet.TYPE_IPV4) {
            IPv4 ip = (IPv4) eth.getPayload();
            Ip4Address srcIP = Ip4Address.valueOf(ip.getSourceAddress());
            Ip4Address dstIp = Ip4Address.valueOf(ip.getDestinationAddress());
            if (ip.getProtocol() == IPv4.PROTOCOL_UDP && dstIp.isMulticast()) {

                log.info("received UDP && multicast");

                if (!first) {
                    return;
                } else {
                    first = false;
                }

                List<NprNfvTypes.Type> sfc = new ArrayList<>();
                sfc.add(NprNfvTypes.Type.TRANSCODER);
                sfc.add(NprNfvTypes.Type.INTRUSION_DETECTION);

                TopologyGraph graph = topoService.getGraph(topoService.currentTopology());

                // VNF deployment cost
                Map<DeviceId, Map<NprNfvTypes.Type, Integer>> vnfDeploymentCost = new HashMap<>();

                Map<NprNfvTypes.Type, Integer> typeToCost = new HashMap<>();
                for (NprNfvTypes.Type type : NprNfvTypes.Type.values()) {
                    typeToCost.put(type, 40);
                }

                for (TopologyVertex vert : graph.getVertexes()) {
                    if (vert.deviceId().toString().startsWith("of:0") ||
                            vert.deviceId().toString().startsWith("of:1")) {
                        vnfDeploymentCost.put(vert.deviceId(), typeToCost);
                    }
                }

                // HW Acceleration Factors
                Map<DeviceId, Map<NprNfvTypes.Type, Double>> hwAccelFactors = new HashMap<>();

                for (TopologyVertex vert : graph.getVertexes()) {
                    DeviceId deviceId = vert.deviceId();
                    log.info("device id: {}", deviceId);

                    if (!(vert.deviceId().toString().startsWith("of:0") ||
                            vert.deviceId().toString().startsWith("of:1"))) {
                        continue;
                    }

                    Map<NprNfvTypes.Type, Double> typeToFactor = new HashMap<>();
                    for (NprNfvTypes.Type type : NprNfvTypes.Type.values()) {
                        if (type == NprNfvTypes.Type.TRANSCODER) {
                            // DXTT2 and DXT9 offer hw acceleration for transcoder
                            if (deviceId.toString().contains("0000000000000002") ||
                                    deviceId.toString().contains("1000000000000009")) {
                                log.info("setting HW acc for {}", deviceId);
                                typeToFactor.put(type, 0.25);
                            } else {
                                typeToFactor.put(type, 1.0);
                            }
                        } else {
                            typeToFactor.put(type, 1.0);
                        }
                    }
                    hwAccelFactors.put(deviceId, typeToFactor);
                }

                // Resource Capacities
                Map<DeviceId, Map<NprResources, Integer>> resourceCapacity = new HashMap<>();
                for (TopologyVertex vert : graph.getVertexes()) {
                    if (vert.deviceId().toString().startsWith("of:0") ||
                            vert.deviceId().toString().startsWith("of:1")) {
                        Map<NprResources, Integer> resourceToCapacity = new HashMap<>();
                        resourceToCapacity.put(NprResources.CPU_CORES, 6);
                        resourceToCapacity.put(NprResources.RAM_IN_GB, 2);
                        resourceCapacity.put(vert.deviceId(), resourceToCapacity);
                    }
                }

                Set<TopologyVertex> wrappedVertices = new HashSet<>();
                for (TopologyVertex vert : graph.getVertexes()) {
                    DeviceId deviceId = vert.deviceId();
                    if (deviceId.toString().startsWith("of:0") || deviceId.toString().startsWith("of:1")) {
                        wrappedVertices.add(new WrappedPoPVertex(vert, vnfDeploymentCost.get(deviceId), hwAccelFactors.get(deviceId), resourceCapacity
                                .get(deviceId)));
                    } else {
                        wrappedVertices.add(new WrappedVertex(vert));
                    }
                }

                for (TopologyVertex vertex : wrappedVertices) {
                    log.info("{} is pop: {}", vertex.toString(), vertex instanceof WrappedPoPVertex);
                }

                Set<TopologyEdge> wrappedEdges = new HashSet<>();
                for (TopologyEdge edge : graph.getEdges()) {
                    wrappedEdges.add(new DefaultTopologyEdge(findByDevId(edge.src()
                                                                             .deviceId(), wrappedVertices), findByDevId(edge.dst()
                                                                                                                            .deviceId(), wrappedVertices), edge
                                                                     .link()));
                }


                AbstractMulticastGroup group = groupManagementService.getGroupById(new IgmpGroupIdentifier(dstIp));

                if (group == null || group.isEmpty()) { // nothing to do, if group is not existing or empty
                    log.info("Group was null or empty, returning now.");
                    return;
                }

                DeviceId ingressDevId = in.receivedFrom().deviceId();
                TopologyVertex ingress = findByDevId(ingressDevId, wrappedVertices);
                Objects.requireNonNull(ingress, "ingress was null, not contained in wrapped vertex set, " +
                        "ingressDeviceId was" + ingressDevId);
                Set<DeviceId> egressDeviceIds = group.toDeviceIds()
                                                     .stream()
                                                     .filter(v -> !v.equals(ingressDevId))
                                                     .collect(Collectors.toSet());
                Set<TopologyVertex> egress = new HashSet<>();
                for (DeviceId deviceId : egressDeviceIds) {
                    TopologyVertex egressForDevId = findByDevId(deviceId, wrappedVertices);
                    Objects.requireNonNull(ingress, "egress was null, not contained in wrapped vertex set, " +
                            "egress DeviceId was" + deviceId);
                    egress.add(egressForDevId);
                }

                NprTraffic traffic = new NprTraffic(sfc, ingress, egress, 4);

                log.info("traffic: " + traffic.toString());

                List<NprTraffic> trafficList = new ArrayList<>();
                trafficList.add(traffic);

                log.info("vertices: {}", wrappedVertices);
                log.info("edges: {}", graph.getEdges());

                NfvPlacementRequest req = new NfvPlacementRequest(wrappedVertices, wrappedEdges, trafficList, new ConstantLinkWeigher(10, 5));

                log.info("req: {}", req);

                //solver = new TPLSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, true, env, 1.0,
                //                                   log);
                solver = new RefSfcPlacementSolver(true, env, 1.0, log);
                NfvPlacementSolution solution = solver.solve(req);
                log.info("solution: {}", solution);

                first = false;


                /*
                NFV instantiating
                 */
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
                            throw new IllegalStateException(
                                    "Trying to instantiate VNF at vert which is not a PoP (" + type.name() + "@" +
                                            vert.deviceId().toString() + ")");
                        }
                    }
                }

                /*
                tree pushing
                 */
                IGroupMember src = new WrappedHost(hostService.getHostsByIp(srcIP).iterator().next());
                Set<IGroupMember> dsts = new HashSet<>(group.getGroupMembers());
                for (IGroupMember dst : dsts) {
                    if (dst.getIpAddress().equals(src.getIpAddress())) {
                        dsts.remove(dst);
                    }
                }

                DefaultNfvTreeFlowPusher pusher = new DefaultNfvTreeFlowPusher(appId, flowRuleService);

                List<Set<TopologyEdge>> solutionEdges = solution.getLogicalEdgesPerTraffic().get(traffic);

                if (!(solutionEdges.size() == traffic.getSfc().size() + 1)) {
                    throw new IllegalStateException(
                            "wrong solution edges size: " + solutionEdges.size() + ", " + "expected " +
                                    (traffic.getSfc().size() + 1));
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

                NFVPerSourceTree nfvTree = new NFVPerSourceTree(src, solutionLinks, dsts, solutionVnfCps, group);
                pusher.pushTree(nfvTree);
            }
        }
    }

    private TopologyVertex findByDevId(DeviceId deviceId, Set<TopologyVertex> setToSearch) {
        for (TopologyVertex vertex : setToSearch) {
            if (vertex.deviceId().equals(deviceId)) {
                return vertex;
            }
        }
        return null;
    }

    private void installDefaultFlows() {
        // existing devices
        Set<FlowRule> flowRules = new HashSet<>();
        deviceService.getDevices().forEach(d -> flowRules.add(buildDefaultFlowRule(d.id())));
        flowRuleService.applyFlowRules(flowRules.toArray(new FlowRule[0]));

        // new devices
        deviceService.addListener(deviceEvent -> {
            if (deviceEvent.type() != DeviceEvent.Type.DEVICE_ADDED) {
                return;
            }
            flowRuleService.applyFlowRules(buildDefaultFlowRule(deviceEvent.subject().id()));
        });
    }

    private FlowRule buildDefaultFlowRule(DeviceId deviceId) {
        return DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .fromApp(appId)
                              .forTable(0)
                              .makePermanent()
                              .withPriority(FlowRule.MAX_PRIORITY - 2)
                              .withSelector(DefaultTrafficSelector.builder()
                                                                  .matchEthType(Ethernet.TYPE_IPV4)
                                                                  .matchIPProtocol(IPv4.PROTOCOL_UDP)
                                                                  .build())
                              .withTreatment(DefaultTrafficTreatment.builder().setOutput(PortNumber.CONTROLLER).build())
                              .build();
    }

    @Override
    public void event(TopologyEvent topologyEvent) {
        // TODO implement
    }
}
