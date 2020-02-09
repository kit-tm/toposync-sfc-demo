package org.onosproject.nfv.placement.solver.heuristic;

import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.nfv.placement.solver.INfvPlacementSolver;
import org.onosproject.nfv.placement.solver.NfvPlacementRequest;
import org.onosproject.nfv.placement.solver.NfvPlacementSolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.WrappedPoPVertex;
import thesiscode.common.tree.TopologyDijkstra;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// TODO merge with RandomSolver (redundancies!)
public class FixedSolver implements INfvPlacementSolver {
    private Logger log = LoggerFactory.getLogger(FixedSolver.class);

    private DeviceId nfvDeviceId;

    public FixedSolver(DeviceId nfvDeviceId) {
        this.nfvDeviceId = nfvDeviceId;
    }

    @Override
    public NfvPlacementSolution solve(NfvPlacementRequest req) {
        List<NprTraffic> trafficList = req.getTraffic();

        if (trafficList.size() != 1) {
            throw new IllegalArgumentException("only one traffic flow allowed");
        }

        NprTraffic traffic = trafficList.get(0);

        if (traffic.getSfc().size() != 1) {
            throw new IllegalArgumentException("only SFCs of length 1 allowed");
        }

        Set<TopologyVertex> nfvCapableWithDeviceId = req.getVertices()
                                                        .stream()
                                                        .filter(v -> v instanceof WrappedPoPVertex)
                                                        .collect(Collectors.toSet());

        if (nfvCapableWithDeviceId.size() != 1) {
            throw new IllegalStateException("found more than one device with desired deviceId");
        }

        TopologyVertex nfv = nfvCapableWithDeviceId.iterator().next();

        HashMap<NprNfvTypes.Type, Set<TopologyVertex>> placement = new HashMap<>();
        placement.put(traffic.getSfc().get(0), nfvCapableWithDeviceId);

        TopologyDijkstra dijk = new TopologyDijkstra(req.getVertices(), req.getEdges());

        // from ingress to nfv
        Set<TopologyEdge> tree = dijk.computeTree(traffic.getIngressNode().deviceId(), nfvCapableWithDeviceId.stream()
                                                                                                             .map(TopologyVertex::deviceId)
                                                                                                             .collect(Collectors
                                                                                                                              .toSet()));

        log.info("ingress -> nfv = {} -> {}:", traffic.getIngressNode().deviceId(), nfv.deviceId());
        for (TopologyEdge edge : tree) {
            log.info("{} -> {}", edge.src().deviceId(), edge.dst().deviceId());
        }

        // from nfv to egress
        tree.addAll(dijk.computeTree(nfv.deviceId(), traffic.getEgressNodes()
                                                            .stream()
                                                            .map(TopologyVertex::deviceId)
                                                            .collect(Collectors.toSet())));

        Map<NprTraffic, Set<TopologyEdge>> solutionEdges = new HashMap<>();
        solutionEdges.put(traffic, tree);


        log.info("nfv -> egress = {} -> {}:", nfv.deviceId(), traffic.getEgressNodes()
                                                                     .stream()
                                                                     .map(TopologyVertex::deviceId)
                                                                     .collect(Collectors.toSet()));
        for (TopologyEdge edge : dijk.computeTree(nfv.deviceId(), traffic.getEgressNodes()
                                                                         .stream()
                                                                         .map(TopologyVertex::deviceId)
                                                                         .collect(Collectors.toSet()))) {
            log.info("{} -> {}", edge.src().deviceId(), edge.dst().deviceId());
        }

        log.info("placement: {}", placement);

        //return new NfvPlacementSolution(solutionEdges, placement);
        return null;
    }

    @Override
    public double getLastRuntime() {
        return 0; // TODO
    }
}
