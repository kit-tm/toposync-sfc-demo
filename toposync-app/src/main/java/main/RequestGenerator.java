package main;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.placement.solver.NfvPlacementRequest;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ConstantLinkWeigher;
import thesiscode.common.topo.ILinkWeigher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class RequestGenerator {
    private static final List<NprNfvTypes.Type> vnfTypes = new ArrayList<>();
    private static final int BANDWIDTH = 10;
    private static final int DELAY = 5;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topoService;

    private ClientServerLocator clientServerLocator;
    private TopologyGraph graph;

    public RequestGenerator(ClientServerLocator clientServerLocator) {
        this.clientServerLocator = clientServerLocator;
        vnfTypes.add(NprNfvTypes.Type.TRANSCODER);
    }

    public NfvPlacementRequest createRequest() {
        graph = topoService.getGraph(topoService.currentTopology());

        List<NprTraffic> clientServerTraffic = computeTraffic();

        Set<TopologyVertex> vertices = graph.getVertexes();
        Set<TopologyEdge> edges = graph.getEdges();

        // TODO wrap vertices (PoPs etc)

        Map<DeviceId, Map<NprNfvTypes.Type, Integer>> vnfDeploymentCost = computeDeploymentCosts(graph.getVertexes());


        ILinkWeigher linkWeigher = new ConstantLinkWeigher(BANDWIDTH, DELAY);

        return new NfvPlacementRequest(vertices, edges, clientServerTraffic, linkWeigher);
    }

    private List<NprTraffic> computeTraffic() {
        final Set<TopologyVertex> vertices = graph.getVertexes();

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


    private TopologyVertex findByDevId(DeviceId deviceId, Set<TopologyVertex> setToSearch) {
        for (TopologyVertex vertex : setToSearch) {
            if (vertex.deviceId().equals(deviceId)) {
                return vertex;
            }
        }
        return null;
    }
}
