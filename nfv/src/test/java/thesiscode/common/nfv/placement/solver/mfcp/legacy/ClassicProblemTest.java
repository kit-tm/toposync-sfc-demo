package thesiscode.common.nfv.placement.solver.mfcp.legacy;

import org.junit.Test;
import org.onosproject.net.DeviceId;
import thesiscode.common.nfv.placement.NfvPlacementTest;
import thesiscode.common.nfv.placement.solver.INfvPlacementSolver;
import thesiscode.common.nfv.placement.solver.NfvPlacementRequest;
import thesiscode.common.nfv.placement.solver.NfvPlacementSolution;
import thesiscode.common.nfv.placement.solver.OptimizationGoal;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ConstantLinkWeigher;
import thesiscode.common.topo.ILinkWeigher;
import thesiscode.common.util.CustomDemandBuilder;
import util.mock.EvalTopoMockBuilder;
import util.mock.TestGraph;

import java.util.ArrayList;
import java.util.List;

public class ClassicProblemTest extends NfvPlacementTest {

    @Test
    public void testDoubleEdges() {
        INfvPlacementSolver solver = new ClassicProblem(OptimizationGoal.LOAD_BALANCING, true);

        TestGraph graph = EvalTopoMockBuilder.getMockedTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 1);

        CustomDemandBuilder cdb = new CustomDemandBuilder();
        List<NprTraffic> traffic = cdb.setIngress(graph.getById(DeviceId.deviceId("s10")))
                                      .addEgress(graph.getById(DeviceId.deviceId("s6")))
                                      .setDemandValue(9)
                                      .setSfc(new ArrayList<>())
                                      .createDemand()
                                      .generateDemand();

        NfvPlacementRequest req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), traffic, lw);

        NfvPlacementSolution sol = solver.solve(req);

        writeToCsvAndPlot(sol, "/home/felix/Desktop/ba/code/eval/" + System.currentTimeMillis());
    }
}
