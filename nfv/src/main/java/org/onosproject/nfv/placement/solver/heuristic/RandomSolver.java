package org.onosproject.nfv.placement.solver.heuristic;

import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.nfv.placement.solver.INfvPlacementSolver;
import org.onosproject.nfv.placement.solver.NfvPlacementRequest;
import org.onosproject.nfv.placement.solver.NfvPlacementSolution;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.WrappedPoPVertex;
import thesiscode.common.tree.TopologyDijkstra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class RandomSolver implements INfvPlacementSolver {
    @Override
    public NfvPlacementSolution solve(NfvPlacementRequest req) {
        List<NprTraffic> trafficList = req.getTraffic();

        if (trafficList.size() != 1) {
            throw new IllegalArgumentException("only 1 traffic flow allowed");
        }

        NprTraffic traffic = trafficList.get(0);

        if (traffic.getSfc().size() != 1) {
            throw new IllegalArgumentException("only SFCs of length 1 allowed");
        }

        Set<TopologyVertex> nfvNodes = req.getVertices()
                                          .stream()
                                          .filter(v -> v instanceof WrappedPoPVertex)
                                          .collect(Collectors.toSet());
        Random rand = new Random();
        TopologyVertex[] nfvNodeArr = nfvNodes.toArray(new TopologyVertex[0]);
        TopologyVertex randomNfvNode = nfvNodeArr[rand.nextInt(nfvNodes.size())];

        Set<TopologyVertex> nfv = new HashSet<>();
        nfv.add(randomNfvNode);
        HashMap<NprNfvTypes.Type, Set<TopologyVertex>> placement = new HashMap<>();
        placement.put(traffic.getSfc().get(0), nfv);

        // path from ingress to nfv
        TopologyDijkstra dijk = new TopologyDijkstra(req.getVertices(), req.getEdges());
        Set<TopologyEdge> tree = dijk.computeTree(traffic.getIngressNode().deviceId(), nfv.stream()
                                                                                          .map(TopologyVertex::deviceId)
                                                                                          .collect(Collectors.toSet()));

        // tree from nfv to egress
        tree.addAll(dijk.computeTree(nfv.iterator().next().deviceId(), traffic.getEgressNodes()
                                                                              .stream()
                                                                              .map(TopologyVertex::deviceId)
                                                                              .collect(Collectors.toSet())));

        Map<NprTraffic, Set<TopologyEdge>> solutionEdges = new HashMap<>();
        solutionEdges.put(traffic, tree);

        //return new NfvPlacementSolution(solutionEdges, placement);
        return null;
    }

    @Override
    public double getLastRuntime() {
        return 0; // TODO
    }

}
