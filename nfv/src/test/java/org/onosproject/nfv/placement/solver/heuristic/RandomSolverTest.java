package org.onosproject.nfv.placement.solver.heuristic;

import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.nfv.placement.solver.NfvPlacementRequest;
import org.onosproject.nfv.placement.solver.NfvPlacementSolution;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ConstantLinkWeigher;
import util.mock.TestGraph;
import util.mock.TopoRef20MockBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RandomSolverTest {

    @Test
    public void solveTest() {
        System.out.println("**RandomSolver Test:");
        TestGraph tg = TopoRef20MockBuilder.getMockedTopo();

        RandomSolver solver = new RandomSolver();

        HashMap<TopologyVertex, Integer> nfvCapableCost = new HashMap<>();
        for (TopologyVertex vertex : tg.getVertexes()) {
            nfvCapableCost.put(vertex, 10);
        }

        List<NprNfvTypes.Type> sfc = new ArrayList<>();
        sfc.add(NprNfvTypes.Type.TRANSCODER);

        TopologyVertex ingress = Objects.requireNonNull(tg.getById(DeviceId.deviceId("s7")));

        Set<TopologyVertex> egress = new LinkedHashSet<>();
        egress.add(Objects.requireNonNull(tg.getById(DeviceId.deviceId("s1"))));
        egress.add(Objects.requireNonNull(tg.getById(DeviceId.deviceId("s6"))));
        egress.add(Objects.requireNonNull(tg.getById(DeviceId.deviceId("s8"))));

        NprTraffic traffic = new NprTraffic(sfc, ingress, egress);
        List<NprTraffic> trafficList = new ArrayList<>();
        trafficList.add(traffic);

        NfvPlacementRequest req = new NfvPlacementRequest(tg.getVertexes(), tg.getEdges(), trafficList, new ConstantLinkWeigher());

        NfvPlacementSolution sol = solver.solve(req);

        //System.out.println(sol.getPlacements());
        for (TopologyEdge edge : sol.getSolutionEdgesByTraffic(traffic)) {
            System.out.println(edge.src().deviceId() + " -> " + edge.dst().deviceId());
        }

    }
}
