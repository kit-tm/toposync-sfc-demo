package main;

import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.placement.solver.NfvPlacementRequest;
import thesiscode.common.nfv.traffic.*;
import thesiscode.common.topo.*;

import java.util.*;


public class RequestGenerator {
    private static final List<NprNfvTypes.Type> vnfTypes = new ArrayList<>();
    private static final int BANDWIDTH = 10;
    private static final int DELAY = 5;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TopologyService topoService;
    private ClientServerLocator clientServerLocator;
    private TopologyGraph graph;

    public RequestGenerator(ClientServerLocator clientServerLocator, TopologyService topoService) {
        this.topoService = topoService;
        this.clientServerLocator = clientServerLocator;
        vnfTypes.add(NprNfvTypes.Type.TRANSCODER);
    }

    public NfvPlacementRequest createRequest() {
        Objects.requireNonNull(topoService);
        Topology topo = Objects.requireNonNull(topoService.currentTopology());

        graph = Objects.requireNonNull(topoService.getGraph(topo));

        final Set<TopologyVertex> vertices = graph.getVertexes();
        final Set<TopologyEdge> edges = graph.getEdges();

        final Map<DeviceId, Map<NprNfvTypes.Type, Integer>> vnfDeploymentCost = computeDeploymentCosts(vertices);
        logger.info("computed vnfDeploymentCost: {}", vnfDeploymentCost);
        final Map<DeviceId, Map<NprResources, Integer>> resourceCapacity = computeResourceCapacities(vertices);
        logger.info("computed resourceCapacity: {}", resourceCapacity);
        final Map<DeviceId, Map<NprNfvTypes.Type, Double>> hwAccelFactors = computeHwAccelFactors(vertices);
        logger.info("computed hwAccelFactors: {}", hwAccelFactors);


        Set<TopologyVertex> wrappedVertices = wrapVertices(vertices, vnfDeploymentCost, hwAccelFactors, resourceCapacity);
        Set<TopologyEdge> wrappedEdges = wrapEdges(wrappedVertices, edges);

        final List<NprTraffic> clientServerTraffic = computeTraffic(wrappedVertices);
        logger.info("computed traffic: {}", clientServerTraffic);

        ILinkWeigher linkWeigher = new ConstantLinkWeigher(BANDWIDTH, DELAY);

        return new NfvPlacementRequest(wrappedVertices, wrappedEdges, clientServerTraffic, linkWeigher);
    }


    private List<NprTraffic> computeTraffic(Set<TopologyVertex> vertices) {

        TopologyVertex server = Objects.requireNonNull(findByDevId(clientServerLocator.getServer()
                                                                                      .location()
                                                                                      .deviceId(), vertices));

        TopologyVertex client1 = Objects.requireNonNull(findByDevId(clientServerLocator.getClient1()
                                                                                       .location()
                                                                                       .deviceId(), vertices));
        TopologyVertex client2 = Objects.requireNonNull(findByDevId(clientServerLocator.getClient2()
                                                                                       .location()
                                                                                       .deviceId(), vertices));

        Set<TopologyVertex> clients = new HashSet<>();
        clients.add(client1);
        clients.add(client2);

        List<NprTraffic> traffics = new ArrayList<>();
        traffics.add(new NprTraffic(vnfTypes, server, clients, 4));

        return traffics;
    }

    private Map<DeviceId, Map<NprNfvTypes.Type, Integer>> computeDeploymentCosts(Set<TopologyVertex> vertices) {
        Map<DeviceId, Map<NprNfvTypes.Type, Integer>> vnfDeploymentCost = new HashMap<>();

        Map<NprNfvTypes.Type, Integer> typeToCost = new HashMap<>();
        for (NprNfvTypes.Type type : NprNfvTypes.Type.values()) {
            typeToCost.put(type, 40);
        }

        for (TopologyVertex vert : vertices) {
            if (vert.deviceId().toString().startsWith("of:0") || vert.deviceId().toString().startsWith("of:1")) {
                vnfDeploymentCost.put(vert.deviceId(), typeToCost);
            }
        }
        return vnfDeploymentCost;
    }

    private Map<DeviceId, Map<NprResources, Integer>> computeResourceCapacities(Set<TopologyVertex> vertices) {
        Map<DeviceId, Map<NprResources, Integer>> resourceCapacity = new HashMap<>();
        for (TopologyVertex vert : vertices) {
            if (vert.deviceId().toString().startsWith("of:0") || vert.deviceId().toString().startsWith("of:1")) {
                Map<NprResources, Integer> resourceToCapacity = new HashMap<>();
                resourceToCapacity.put(NprResources.CPU_CORES, 6);
                resourceToCapacity.put(NprResources.RAM_IN_GB, 2);
                resourceCapacity.put(vert.deviceId(), resourceToCapacity);
            }
        }
        return resourceCapacity;
    }

    private Map<DeviceId, Map<NprNfvTypes.Type, Double>> computeHwAccelFactors(Set<TopologyVertex> vertices) {
        Map<NprNfvTypes.Type, Double> noAccel = new HashMap<>();
        for (NprNfvTypes.Type type : NprNfvTypes.Type.values()) {
            noAccel.put(type, 1.0);
        }

        Map<DeviceId, Map<NprNfvTypes.Type, Double>> hwAccelFactors = new HashMap<>();
        for (TopologyVertex vert : vertices) {
            hwAccelFactors.put(vert.deviceId(), noAccel);
        }
        return hwAccelFactors;
    }

    private Set<TopologyVertex> wrapVertices(Set<TopologyVertex> vertices, Map<DeviceId, Map<NprNfvTypes.Type, Integer>> vnfDeploymentCost, Map<DeviceId, Map<NprNfvTypes.Type, Double>> hwAccelFactors, Map<DeviceId, Map<NprResources, Integer>> resourceCapacity) {
        Set<TopologyVertex> wrappedVertices = new HashSet<>();
        for (TopologyVertex vert : vertices) {
            DeviceId deviceId = vert.deviceId();
            if (deviceId.toString().startsWith("of:0") || deviceId.toString().startsWith("of:1")) {
                wrappedVertices.add(new WrappedPoPVertex(vert, vnfDeploymentCost.get(deviceId), hwAccelFactors.get(deviceId), resourceCapacity
                        .get(deviceId)));
            } else {
                wrappedVertices.add(new WrappedVertex(vert));
            }
        }
        return wrappedVertices;
    }

    private Set<TopologyEdge> wrapEdges(Set<TopologyVertex> wrappedVertices, Set<TopologyEdge> edges) {
        Set<TopologyEdge> wrappedEdges = new HashSet<>();
        for (TopologyEdge edge : graph.getEdges()) {
            wrappedEdges.add(new DefaultTopologyEdge(findByDevId(edge.src()
                                                                     .deviceId(), wrappedVertices), findByDevId(edge.dst()
                                                                                                                    .deviceId(), wrappedVertices), edge
                                                             .link()));
        }
        return wrappedEdges;
    }

    private TopologyVertex findByDevId(DeviceId deviceId, Set<TopologyVertex> setToSearch) {
        for (TopologyVertex vertex : setToSearch) {
            if (vertex.deviceId().equals(deviceId)) {
                return vertex;
            }
        }
        return null;
    }
}
