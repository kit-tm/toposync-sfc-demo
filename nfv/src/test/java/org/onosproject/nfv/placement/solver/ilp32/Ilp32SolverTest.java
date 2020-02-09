package org.onosproject.nfv.placement.solver.ilp32;

import org.junit.Assert;
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

public class Ilp32SolverTest {

    @Test
    public void solveTest() {
        System.out.println("**Ilp32Solver Test:");
        TestGraph mockedGraph = TopoRef20MockBuilder.getMockedTopo();

        List<NprNfvTypes.Type> sfc = new ArrayList<>();
        sfc.add(NprNfvTypes.Type.TRANSCODER);

        TopologyVertex ingress = Objects.requireNonNull(mockedGraph.getById(DeviceId.deviceId("s7")));

        Set<TopologyVertex> egress = new LinkedHashSet<>();
        egress.add(Objects.requireNonNull(mockedGraph.getById(DeviceId.deviceId("s1"))));
        egress.add(Objects.requireNonNull(mockedGraph.getById(DeviceId.deviceId("s6"))));
        egress.add(Objects.requireNonNull(mockedGraph.getById(DeviceId.deviceId("s8"))));


        NprTraffic traffic = new NprTraffic(sfc, ingress, egress);
        List<NprTraffic> trafficList = new ArrayList<>();
        trafficList.add(traffic);

        HashMap<TopologyVertex, Integer> nfvCapableCost = new HashMap<>();
        for (TopologyVertex vertex : mockedGraph.getVertexes()) {
            nfvCapableCost.put(vertex, 10);
        }

        NfvPlacementRequest req = new NfvPlacementRequest(mockedGraph.getVertexes(), mockedGraph.getEdges(), trafficList, new ConstantLinkWeigher());

        Ilp32Solver solver = new Ilp32Solver();
        NfvPlacementSolution solution = solver.solve(req);

        System.out.println("Solution tree:");
        for (TopologyEdge edge : solution.getSolutionEdgesByTraffic(traffic)) {
            System.out.println(edge.src().deviceId() + " -> " + edge.dst().deviceId());
        }

        System.out.println("Solution placement:");
        // System.out.println(solution.getPlacements());

        HardCodedIlp32TopoRef20.init();
        Assert.assertEquals(2 * HardCodedIlp32TopoRef20.getK(), solver.getK());
        Assert.assertEquals(HardCodedIlp32TopoRef20.getN(), solver.getN());
        Assert.assertEquals(HardCodedIlp32TopoRef20.getn(), solver.getn());

        Assert.assertArrayEquals("P1 not equal", HardCodedIlp32TopoRef20.getP1(), solver.getP1(), 0d);
        //Assert.assertArrayEquals("P2 not equal", HardCodedIlp32TopoRef20.getP2(), solver.getP2());
        Assert.assertArrayEquals("P3 not equal", HardCodedIlp32TopoRef20.getP3(), solver.getP3(), 0d);

        Assert.assertArrayEquals("M not equal", HardCodedIlp32TopoRef20.getM(), solver.getM());
        Assert.assertArrayEquals("A not equal", HardCodedIlp32TopoRef20.getA(), solver.getA());
        Assert.assertArrayEquals("W not equal", HardCodedIlp32TopoRef20.getW(), solver.getW());
        Assert.assertArrayEquals("R not equal", HardCodedIlp32TopoRef20.getR(), solver.getR());
        Assert.assertArrayEquals("Q not equal", HardCodedIlp32TopoRef20.getQ(), solver.getQ());
        Assert.assertArrayEquals("S not equal", HardCodedIlp32TopoRef20.getS(), solver.getS());
    }


}
