package thesiscode.common.nfv.placement.solver.mfcp.used;

import gurobi.GRBEnv;
import gurobi.GRBException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import thesiscode.common.nfv.placement.NfvPlacementTest;
import thesiscode.common.nfv.placement.solver.INfvPlacementSolver;
import thesiscode.common.nfv.placement.solver.NfvPlacementRequest;
import thesiscode.common.nfv.placement.solver.NfvPlacementSolution;
import thesiscode.common.nfv.placement.solver.OptimizationGoal;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ConstantLinkWeigher;
import thesiscode.common.topo.ILinkWeigher;
import thesiscode.common.util.CustomDemandBuilder;
import thesiscode.common.util.RandomDemandGenerator;
import util.mock.EvalTopoMockBuilder;
import util.mock.TestGraph;

import java.util.ArrayList;
import java.util.List;

public class NoSfcPlacementSolverTest extends NfvPlacementTest {
    private static GRBEnv env;
    private NoSfcPlacementSolver solver;

    @BeforeClass
    public static void setUp() throws GRBException {
        env = new GRBEnv("nfv.log");
    }

    @AfterClass
    public static void tearDown() throws GRBException {
        env.dispose();
    }

    @Test
    public void minMaxDelaySumThenDeviation() {
        TestGraph graph = EvalTopoMockBuilder.getMockedTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 1);

        RandomDemandGenerator dg = new RandomDemandGenerator(graph, lw);

        NfvPlacementRequest req;
        NfvPlacementSolution sol;
        NfvPlacementSolution multiSol;
        double firstDelay;
        for (int i = 0; i < 1000; i++) {
            //System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            List<NprTraffic> traffic = dg.generateDemand();
            req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), traffic, lw);

            solver = new NoSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, true, env);
            sol = solver.solve(req);


            boolean unnecce = false;
            for (NprTraffic flow : traffic) {
                unnecce |= containsUnnecessaryEdges(flow, sol.getSolutionEdgesByTraffic(flow));
            }

            if (unnecce) {
                writeToCsvAndPlot(sol, NfvPlacementTest.CSV_BASE_PATH + System.currentTimeMillis());
                return;
            }

            if (!allEdgesReachableFromSource(sol) || !destinationsReachable(sol)) {
                writeToCsvAndPlot(sol, NfvPlacementTest.CSV_BASE_PATH + System.currentTimeMillis());
                return;
            }

            System.out.println(solver.getLastRuntime());

            if (i % 10 == 0) {
                System.out.println(i);
            }

        }
    }


    //@Test
    public void testMinimalNoSfc() {

        TestGraph graph = EvalTopoMockBuilder.getMockedTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 1);

        ArrayList<NprNfvTypes.Type> sfc = new ArrayList<>();

        CustomDemandBuilder cdb = new CustomDemandBuilder();
        List<NprTraffic> demand = cdb.setIngress(graph.getById(DeviceId.deviceId("s9")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s6")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s10")))
                                     .setDemandValue(10)
                                     .setSfc(sfc)
                                     .createDemand()
                                     .generateDemand();

        /*List<NprTraffic> demand = cdb.setIngress(graph.getById(DeviceId.deviceId("s6")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s1")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s9")))
                                     .setDemandValue(10.0)
                                     .setSfc(sfc)
                                     .createDemand()
                                     .generateDemand();*/


        NfvPlacementRequest req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), demand, lw);

        INfvPlacementSolver solver = new NoSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, true, env);

        NfvPlacementSolution sol = solver.solve(req);

        writeToCsvAndPlot(sol, "/home/felix/Desktop/ba/code/eval/" + System.currentTimeMillis());
    }

    //@Test
    public void bla() {
        TestGraph graph = EvalTopoMockBuilder.getMockedTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 1);

        RandomDemandGenerator dg = new RandomDemandGenerator(graph, lw);


        NfvPlacementRequest req;
        NfvPlacementSolution sol;

        double deviationMinMaxSum;

        double maxDelayMinMaxSum;


        for (int i = 0; i < 1000; i++) {
            req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), dg.generateDemand(), lw);

            solver = new NoSfcPlacementSolver(OptimizationGoal.DELAY_REDUCTION_PER_DST_SUM, false, env);
            sol = solver.solve(req);
            //System.out.println(solver.getLastRuntime());
            plotIfNotExpected(sol, this::destinationsReachable, true,
                              "/home/felix/Desktop/ba/code/eval/" + System.currentTimeMillis());
            deviationMinMaxSum = sol.getDeviationSum();

            if (!allEdgesReachableFromSource(sol)) {
                System.out.println("not all edges reachable from src :(");
                writeToCsvAndPlot(sol, "/home/felix/Desktop/ba/code/eval/sol" + System.currentTimeMillis());
                return;
            }

            solver = new NoSfcPlacementSolver(OptimizationGoal.DELAY_REDUCTION_PER_DST_SUM_MULTI, false, env);

            NfvPlacementSolution sol2 = solver.solve(req);
            // System.out.println(solver.getLastRuntime());
            plotIfNotExpected(sol, this::destinationsReachable, true,
                              "/home/felix/Desktop/ba/code/eval/" + System.currentTimeMillis());

            System.out.println(deviationMinMaxSum + "," + sol2.getDeviationSum());

            if (!allEdgesReachableFromSource(sol)) {
                System.out.println("not all edges reachable from src :(");
                writeToCsvAndPlot(sol, "/home/felix/Desktop/ba/code/eval/sol" + System.currentTimeMillis());
                return;
            }

            //System.out.println("finished");

        }
    }

    //@Test
    public void testUnnecessaryEdges() {
        TestGraph graph = EvalTopoMockBuilder.getMockedTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 1);

        CustomDemandBuilder cdb = new CustomDemandBuilder();
        List<NprTraffic> traffic = cdb.setIngress(graph.getById(DeviceId.deviceId("s3")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s7")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s4")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s6")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s9")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s10")))
                                      .setDemandValue(2)
                                      .setSfc(new ArrayList<>())
                                      .createDemand()
                                      .setIngress(graph.getById(DeviceId.deviceId("s9")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s6")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s5")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s8")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s7")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s1")))
                                      .setDemandValue(1)
                                      .setSfc(new ArrayList<>())
                                      .createDemand()
                                      .setIngress(graph.getById(DeviceId.deviceId("s4")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s6")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s9")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s5")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s3")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s1")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s7")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s8")))
                                      .setDemandValue(7)
                                      .setSfc(new ArrayList<>())
                                      .createDemand()
                                      .generateDemand();

        NfvPlacementRequest req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), traffic, lw);

        NoSfcPlacementSolver solver;

        for (int i = 0; i < 1; i++) {
            solver = new NoSfcPlacementSolver(OptimizationGoal.DELAY_REDUCTION_PER_DST_SUM_MULTI, false, env);

            NfvPlacementSolution sol = solver.solve(req);

            System.out.println(sol.getDeviationSum());

            plotIfNotExpected(sol, this::destinationsReachable, true,
                              "/home/felix/Desktop/ba/code/eval/" + System.currentTimeMillis());

            writeToCsvAndPlot(sol, "/home/felix/Desktop/ba/code/eval/" + System.currentTimeMillis());

        }
    }

}
