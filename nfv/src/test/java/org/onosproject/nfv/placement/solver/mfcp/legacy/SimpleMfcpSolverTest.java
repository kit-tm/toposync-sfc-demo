package org.onosproject.nfv.placement.solver.mfcp.legacy;

import gurobi.GRBEnv;
import gurobi.GRBException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.nfv.placement.NfvPlacementTest;
import org.onosproject.nfv.placement.solver.AbstractNfvIlpPlacementSolver;
import org.onosproject.nfv.placement.solver.INfvPlacementSolver;
import org.onosproject.nfv.placement.solver.NfvPlacementRequest;
import org.onosproject.nfv.placement.solver.NfvPlacementSolution;
import org.onosproject.nfv.placement.solver.OptimizationGoal;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ConstantLinkWeigher;
import thesiscode.common.topo.ILinkWeigher;
import thesiscode.common.util.CustomDemandBuilder;
import thesiscode.common.util.IDemandGenerator;
import thesiscode.common.util.ScaleDemandGenerator;
import util.mock.EvalTopoMockBuilder;
import util.mock.TestGraph;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class SimpleMfcpSolverTest extends NfvPlacementTest {
    private static GRBEnv env;
    private AbstractNfvIlpPlacementSolver solver;

    @BeforeClass
    public static void setUp() throws GRBException {
        env = new GRBEnv("nfv.log");
    }

    //@Test
    public void solveLoadBalancing() throws GRBException {
        solver = new SimpleMfcpSolver(OptimizationGoal.LOAD_BALANCING, false, env);

        for (int i = 0; i < 1000; i++) {
            solveRandomRequest(solver, false);
        }
    }

    //@Test
    public void solveLoadReduction() {
        solver = new SimpleMfcpSolver(OptimizationGoal.LOAD_REDUCTION, false, env);
        for (int i = 0; i < 100000; i++) {
            solveRandomRequest(solver, false);
        }
    }

    @Test
    public void solveLoadReductionPerDest() {
        solver = new SimpleMfcpSolver(OptimizationGoal.LOAD_REDUCTION, false, env);
        for (int i = 0; i < 100000; i++) {
            solveRandomRequest(solver, false);
        }
    }

    //@Test
    public void solveBalancingThenReduction() {
        solver = new SimpleMfcpSolver(OptimizationGoal.BALANCE_THEN_REDUCTION, false, env);
        for (int i = 0; i < 1000; i++) {
            solveRandomRequest(solver, false);
        }
    }

    //@Test
    public void solveReductionThenBalancing() {
        solver = new SimpleMfcpSolver(OptimizationGoal.REDUCTION_THEN_BALANCE, false, env);
        for (int i = 0; i < 1000; i++) {
            solveRandomRequest(solver, false);
        }
    }


    //@Test
    public void reproduce() throws GRBException {
        INfvPlacementSolver solver = new SimpleMfcpSolver(OptimizationGoal.LOAD_BALANCING, true, env);

        TestGraph graph = EvalTopoMockBuilder.getMockedTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 1);

        CustomDemandBuilder cdb = new CustomDemandBuilder();
        List<NprTraffic> traffic = cdb.setIngress(graph.getById(DeviceId.deviceId("s6")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s3")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s5")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s10")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s7")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s8")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s9")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s1")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s2")))
                                      .setDemandValue(8)
                                      .setSfc(new ArrayList<>())
                                      .createDemand()
                                      .setIngress(graph.getById(DeviceId.deviceId("s7")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s3")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s4")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s5")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s10")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s8")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s9")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s2")))
                                      .setDemandValue(2)
                                      .setSfc(new ArrayList<>())
                                      .createDemand()
                                      .generateDemand();

        NfvPlacementRequest req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), traffic, lw);

        NfvPlacementSolution sol = solver.solve(req);

        for (NprTraffic trafficFlow : traffic) {
            Assert.assertTrue("not all destinations reachable :-(", destinationsReachable(sol));
        }

        writeToCsvAndPlot(sol, "/home/felix/Desktop/ba/code/eval/" + System.currentTimeMillis());
    }
}
