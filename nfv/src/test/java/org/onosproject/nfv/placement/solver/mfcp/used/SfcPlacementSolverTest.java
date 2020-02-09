package org.onosproject.nfv.placement.solver.mfcp.used;

import gurobi.GRBEnv;
import gurobi.GRBException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.nfv.placement.NfvPlacementTest;
import org.onosproject.nfv.placement.solver.INfvPlacementSolver;
import org.onosproject.nfv.placement.solver.NfvPlacementRequest;
import org.onosproject.nfv.placement.solver.NfvPlacementSolution;
import org.onosproject.nfv.placement.solver.OptimizationGoal;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ConstantLinkWeigher;
import thesiscode.common.topo.ILinkWeigher;
import thesiscode.common.util.CustomDemandBuilder;
import util.mock.EvalTopoMockBuilder;
import util.mock.TestGraph;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SfcPlacementSolverTest extends NfvPlacementTest {
    private GRBEnv env;

    /*private static final int X_START = 16;
    private static final int X_END = 5 * 16;
    private static final int X_STEP = 4;*/

    private static final int X_START = 16;
    private static final int X_END = 3 * 16;
    private static final int X_STEP = 4;


    @Before
    public void setUp() throws GRBException {
        env = new GRBEnv("");
    }

    /**
     * used for the first evaluation experiments
     *
     * @throws IOException if opening/writing to the CSV files fails
     */
    //@Test
    public void testRandom() throws IOException {
        TestGraph graph = EvalTopoMockBuilder.getEvalTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 5);

        PrintWriter networkLoadPrintWriter = new PrintWriter(new FileWriter("network_load.csv", true));
        PrintWriter delaySumPrintWriter = new PrintWriter(new FileWriter("delay_sum.csv", true));
        PrintWriter deviationSumPrintWriter = new PrintWriter(new FileWriter("deviation_sum.csv", true));

        List<TopologyVertex> ingressList = new ArrayList<>();
        List<Set<TopologyVertex>> egressList = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            // choose random ingress
            Set<TopologyVertex> tbsSet = new HashSet<>(graph.getTbs());
            Set<TopologyVertex> dxtSet = new HashSet<>(graph.getDxt());
            TopologyVertex[] availableTbsArr = tbsSet.toArray(new TopologyVertex[0]);
            TopologyVertex[] availableDxtArr = dxtSet.toArray(new TopologyVertex[0]);

            if (ThreadLocalRandom.current().nextInt(100) < 25) {
                // source = DXT
                int index = ThreadLocalRandom.current().nextInt(availableDxtArr.length);
                TopologyVertex ingress = availableDxtArr[index];
                ingressList.add(ingress);
            } else {
                // source = TBS
                int index = ThreadLocalRandom.current().nextInt(availableTbsArr.length);
                TopologyVertex ingress = availableTbsArr[index];
                tbsSet.remove(ingress);
                availableTbsArr = tbsSet.toArray(new TopologyVertex[0]);
                ingressList.add(ingress);
            }


            // choose random egress
            Set<TopologyVertex> currentEgress = new HashSet<>();
            while (currentEgress.size() < 3) {
                int index = ThreadLocalRandom.current().nextInt(availableTbsArr.length);
                TopologyVertex chosenEgress = availableTbsArr[index];
                if (chosenEgress.deviceId().toString().startsWith("tbs")) {
                    currentEgress.add(chosenEgress);
                }
                tbsSet.remove(chosenEgress);
                availableTbsArr = tbsSet.toArray(new TopologyVertex[0]);
            }
            egressList.add(currentEgress);

        }

        ArrayList<Double> loadListRef = new ArrayList<>();
        ArrayList<Double> deviationListRef = new ArrayList<>();
        ArrayList<Double> delayListRef = new ArrayList<>();
        ArrayList<Double> loadListTpl = new ArrayList<>();
        ArrayList<Double> deviationListTpl = new ArrayList<>();
        ArrayList<Double> delayListTpl = new ArrayList<>();
        ArrayList<Double> loadListSt = new ArrayList<>();
        ArrayList<Double> deviationListSt = new ArrayList<>();
        ArrayList<Double> delayListSt = new ArrayList<>();

        for (int i = 0; i <= 2; i++) {
            CustomDemandBuilder dg = new CustomDemandBuilder();
            for (int j = 0; j < 3; j++) {
                dg.setIngress(ingressList.get(j))
                  .setEgress(egressList.get(j))
                  .setDemandValue(4)
                  .setRandomSfc(i)
                  .createDemand();
            }


            List<NprTraffic> demand = dg.generateDemand();
            System.out.println("finished building demand, here it is:");

            for (NprTraffic flow : demand) {
                System.out.println(flow.toString());
            }

            NfvPlacementRequest req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), demand, lw);
            SfcPlacementSolver refSolver = new RefSfcPlacementSolver(false, env, 1.0);
            NfvPlacementSolution refSol = refSolver.solve(req);
            if (refSol == null) {
                System.out.println("refSol was not feasible, skip");
                Assert.fail();
            }
            System.out.println("ref was feasible");


            SfcPlacementSolver tplSolver = new TPLSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, false, env, 1.0);
            NfvPlacementSolution tplSol = tplSolver.solve(req);
            if (tplSol == null) {
                System.out.println("TPL was not feasible, skip");
                Assert.fail();
            }
            System.out.println("TPL was feasible");

            SfcPlacementSolver stSolver = new STSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, false, env, 1.0);
            NfvPlacementSolution stSol = stSolver.solve(req);
            if (stSol == null) {
                System.out.println("ST was not feasible, skip");
                Assert.fail();
            }
            System.out.println("ST was feasible");


            System.out.println("modelling times: " + tplSolver.getModelTime() + "," + stSolver.getModelTime());
            System.out.println("runtimes: " + tplSolver.getLastRuntime() + "," + stSolver.getLastRuntime());
            System.out.println("load:" + refSol.getNetworkLoad() + " vs " + tplSol.getNetworkLoad() + " vs. " +
                                       stSol.getNetworkLoad());
            System.out.println("delaySum:" + refSol.getDelaySum() + " vs. " + tplSol.getDelaySum() + " vs. " +
                                       stSol.getDelaySum());
            System.out.println(
                    "deviationSum:" + refSol.getDeviationSum() + " vs. " + tplSol.getDeviationSum() + " vs. " +
                            stSol.getDeviationSum());

            loadListRef.add(refSol.getNetworkLoad());
            loadListTpl.add(tplSol.getNetworkLoad());
            loadListSt.add(stSol.getNetworkLoad());

            deviationListRef.add(refSol.getDeviationSum());
            deviationListTpl.add(tplSol.getDeviationSum());
            deviationListSt.add(stSol.getDeviationSum());

            delayListRef.add(refSol.getDelaySum());
            delayListTpl.add(tplSol.getDelaySum());
            delayListSt.add(stSol.getDelaySum());

            if (i == 2) {
                System.out.println("all were feasible, writing to file");

                for (int x = 0; x < loadListRef.size(); x++) {
                    networkLoadPrintWriter.append(String.valueOf(loadListRef.get(x)))
                                          .append(",")
                                          .append(String.valueOf(loadListTpl.get(x)))
                                          .append(",")
                                          .append(String.valueOf(loadListSt.get(x)))
                                          .append("\n");
                }
                networkLoadPrintWriter.flush();

                for (int x = 0; x < delayListRef.size(); x++) {
                    delaySumPrintWriter.append(String.valueOf(delayListRef.get(x)))
                                       .append(",")
                                       .append(String.valueOf(delayListTpl.get(x)))
                                       .append(',')
                                       .append(String.valueOf(delayListSt.get(x)))
                                       .append("\n");
                }
                delaySumPrintWriter.flush();

                for (int x = 0; x < deviationListRef.size(); x++) {
                    deviationSumPrintWriter.append(String.valueOf(deviationListRef.get(x)))
                                           .append(",")
                                           .append(String.valueOf(deviationListTpl.get(x)))
                                           .append(',')
                                           .append(String.valueOf(deviationListSt.get(x)))
                                           .append("\n");
                }
                deviationSumPrintWriter.flush();
            }
        }
    }


    //@Test
    public void getGoodExample() {
        TestGraph tg = EvalTopoMockBuilder.getEvalTopo();
        ILinkWeigher lw = new ConstantLinkWeigher(10, 5);
        CustomDemandBuilder cdb = new CustomDemandBuilder();
        List<NprNfvTypes.Type> sfc = new ArrayList<>();
        sfc.add(NprNfvTypes.Type.TRANSCODER);
        sfc.add(NprNfvTypes.Type.INTRUSION_DETECTION);

        Set<TopologyVertex> egress = new HashSet<>();
        egress.add(tg.getById(DeviceId.deviceId("tbs313")));
        egress.add(tg.getById(DeviceId.deviceId("tbs212")));
        egress.add(tg.getById(DeviceId.deviceId("tbs412")));
        egress.add(tg.getById(DeviceId.deviceId("tbs411")));
        List<NprTraffic> traffic = cdb.setSfc(sfc)
                                      .setDemandValue(4)
                                      .setIngress(tg.getById(DeviceId.deviceId("tbs321")))
                                      .setEgress(egress)
                                      .createDemand()
                                      .generateDemand();
        System.out.println("this is the traffic:");
        for (NprTraffic call : traffic) {
            System.out.println(call.toString());
        }

        NfvPlacementRequest req = new NfvPlacementRequest(tg.getVertexes(), tg.getEdges(), traffic, lw);

        SfcPlacementSolver refSolver = new RefSfcPlacementSolver(true, env, 1.0);
        NfvPlacementSolution refSol = refSolver.solve(req);
        if (refSol == null) {
            System.out.println("REF was not feasible");
            Assert.fail();
        }

        SfcPlacementSolver tplSolver = new TPLSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, true, env, 1.0);
        NfvPlacementSolution tplSol = tplSolver.solve(req);
        if (tplSol == null) {
            System.out.println("TPL was not feasible");
            Assert.fail();
        }

        System.out.println("deviation (tpl,ref) = (" + tplSol.getDeviationSum() + "," + refSol.getDeviationSum() + ")");
        System.out.println("delay (tpl,ref) = (" + tplSol.getDelaySum() + "," + refSol.getDelaySum() + ")");

        if (!(tplSol.getDeviationSum() + 10 < refSol.getDeviationSum() &&
                tplSol.getDelaySum() < refSol.getDelaySum())) {
            Assert.fail();
        }


    }

    @Test
    public void runtime() throws IOException {
        TestGraph graph = EvalTopoMockBuilder.getEvalTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 5);

        PrintWriter runTimePrintWriter = new PrintWriter(new FileWriter("runtime.csv", true));

        List<TopologyVertex> ingressList = new ArrayList<>();
        List<Set<TopologyVertex>> egressList = new ArrayList<>();

        for (int i = 0; i < 1; i++) {
            // choose random ingress
            Set<TopologyVertex> tbsSet = new HashSet<>(graph.getTbs());
            Set<TopologyVertex> dxtSet = new HashSet<>(graph.getDxt());
            TopologyVertex[] availableTbsArr = tbsSet.toArray(new TopologyVertex[0]);
            TopologyVertex[] availableDxtArr = dxtSet.toArray(new TopologyVertex[0]);

            if (ThreadLocalRandom.current().nextInt(100) < 25) {
                // source = DXT
                int index = ThreadLocalRandom.current().nextInt(availableDxtArr.length);
                TopologyVertex ingress = availableDxtArr[index];
                ingressList.add(ingress);
            } else {
                // source = TBS
                int index = ThreadLocalRandom.current().nextInt(availableTbsArr.length);
                TopologyVertex ingress = availableTbsArr[index];
                tbsSet.remove(ingress);
                availableTbsArr = tbsSet.toArray(new TopologyVertex[0]);
                ingressList.add(ingress);
            }


            // choose random egress
            Set<TopologyVertex> currentEgress = new HashSet<>();
            while (currentEgress.size() < 3) {
                int index = ThreadLocalRandom.current().nextInt(availableTbsArr.length);
                TopologyVertex chosenEgress = availableTbsArr[index];
                if (chosenEgress.deviceId().toString().startsWith("tbs")) {
                    currentEgress.add(chosenEgress);
                }
                tbsSet.remove(chosenEgress);
                availableTbsArr = tbsSet.toArray(new TopologyVertex[0]);
            }
            egressList.add(currentEgress);

        }

        ArrayList<Double> runTimesTpl = new ArrayList<>();
        ArrayList<Double> runTimesSt = new ArrayList<>();

        for (int i = 0; i <= 2; i++) {
            CustomDemandBuilder dg = new CustomDemandBuilder();
            for (int j = 0; j < 1; j++) {
                dg.setIngress(ingressList.get(j))
                  .setEgress(egressList.get(j))
                  .setDemandValue(4)
                  .setRandomSfc(i)
                  .createDemand();
            }

            List<NprTraffic> demand = dg.generateDemand();

            System.out.println("demand: " + demand.toString());

            NfvPlacementRequest req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), demand, lw);


            SfcPlacementSolver tplSolver = new TPLSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, false, env, 1.0);
            NfvPlacementSolution tplSol = tplSolver.solve(req);
            if (tplSol == null) {
                System.out.println("TPL was not feasible, skip");
                Assert.fail();
            }
            System.out.println("TPL was feasible");

            SfcPlacementSolver stSolver = new STSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, false, env, 1.0);
            NfvPlacementSolution stSol = stSolver.solve(req);
            if (stSol == null) {
                System.out.println("ST was not feasible, skip");
                Assert.fail();
            }
            System.out.println("ST was feasible");


            System.out.println("runtimes: " + (tplSolver.getLastRuntime() + tplSolver.getModelTime()) + "," +
                                       (stSolver.getLastRuntime() + stSolver.getModelTime()));

            runTimesTpl.add(tplSolver.getLastRuntime() + tplSolver.getModelTime());
            runTimesSt.add(stSolver.getLastRuntime() + stSolver.getModelTime());

            if (i == 2) {
                System.out.println("all were feasible, writing to file");

                for (int x = 0; x < runTimesTpl.size(); x++) {
                    runTimePrintWriter.append(String.valueOf(runTimesTpl.get(x)))
                                      .append(",")
                                      .append(String.valueOf(runTimesSt.get(x)))
                                      .append("\n");
                }
                runTimePrintWriter.flush();
            }
        }
    }

    /**
     * used for the tradeoff eval experiments
     *
     * @throws IOException
     */
    //@Test
    public void tradeoffLoadConstraint() throws IOException {
        TestGraph graph = EvalTopoMockBuilder.getEvalTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 5);

        PrintWriter deviationWriterTpl = new PrintWriter(new FileWriter("tradeoff_deviation_tpl.csv", true));
        PrintWriter loadWriterTpl = new PrintWriter(new FileWriter("tradeoff_load_tpl.csv", true));
        PrintWriter deviationWriterSt = new PrintWriter(new FileWriter("tradeoff_deviation_st.csv", true));
        PrintWriter loadWriterSt = new PrintWriter(new FileWriter("tradeoff_load_st.csv", true));

        for (int x = 0; x < 1000; x++) {
            // choose random ingress
            TopologyVertex ingress = null;
            Set<TopologyVertex> tbsSet = new HashSet<>(graph.getTbs());
            Set<TopologyVertex> dxtSet = new HashSet<>(graph.getDxt());
            TopologyVertex[] availableTbsArr = tbsSet.toArray(new TopologyVertex[0]);
            TopologyVertex[] availableDxtArr = dxtSet.toArray(new TopologyVertex[0]);

            if (ThreadLocalRandom.current().nextInt(100) < 25) {
                // source = DXT
                int index = ThreadLocalRandom.current().nextInt(availableDxtArr.length);
                ingress = availableDxtArr[index];
            } else {
                // source = TBS
                int index = ThreadLocalRandom.current().nextInt(availableTbsArr.length);
                ingress = availableTbsArr[index];
                tbsSet.remove(ingress);
                availableTbsArr = tbsSet.toArray(new TopologyVertex[0]);
            }

            // choose random egress
            //int egressAmount = ThreadLocalRandom.current().nextInt(3, 6);
            int egressAmount = 3;
            Set<TopologyVertex> egress = new HashSet<>();
            while (egress.size() < egressAmount) {
                int index = ThreadLocalRandom.current().nextInt(availableTbsArr.length);
                TopologyVertex chosenEgress = availableTbsArr[index];
                if (chosenEgress.deviceId().toString().startsWith("tbs")) {
                    egress.add(chosenEgress);
                }
                tbsSet.remove(chosenEgress);
                availableTbsArr = tbsSet.toArray(new TopologyVertex[0]);
            }

            for (int sfcLength = 0; sfcLength <= 2; sfcLength++) {
                // choose random SFC(s)
                NprNfvTypes.Type[] allTypes = NprNfvTypes.Type.values();
                Set<NprNfvTypes.Type> typeSet = new HashSet<>(Arrays.asList(allTypes));
                List<NprNfvTypes.Type> currentSfc = new ArrayList<>();
                for (int i = 0; i < sfcLength; i++) {
                    int index = ThreadLocalRandom.current().nextInt(allTypes.length);
                    NprNfvTypes.Type chosenType = allTypes[index];
                    currentSfc.add(chosenType);
                    typeSet.remove(chosenType);
                    allTypes = typeSet.toArray(new NprNfvTypes.Type[0]);
                }
                List<NprTraffic> traffic = new ArrayList<>();
                traffic.add(new NprTraffic(currentSfc, ingress, egress, 4));
                System.out.println("finished building demand, here it is:");
                System.out.println(traffic.get(0));
                NfvPlacementRequest req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), traffic, lw);

                for (int loadConstraint = X_START; loadConstraint <= X_END; loadConstraint += X_STEP) {
                    boolean tplFeasible = true;
                    boolean stFeasible = true;


                    SfcPlacementSolver tplSolver = new TPLSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, false, env, 1.0, loadConstraint);
                    NfvPlacementSolution tplSol = tplSolver.solve(req);
                    if (tplSol == null) {
                        System.out.println("TPL was not feasible");
                        tplFeasible = false;
                    }

                    SfcPlacementSolver stSolver = new STSfcPlacementSolver(OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION, false, env, 1.0, loadConstraint);
                    NfvPlacementSolution stSol = stSolver.solve(req);
                    if (stSol == null) {
                        System.out.println("ST was not feasible");
                        stFeasible = false;
                    }

                    if (tplFeasible) {
                        deviationWriterTpl.append(String.valueOf(tplSol.getDeviationSum()));
                        loadWriterTpl.append(String.valueOf(tplSol.getNetworkLoad()));

                        System.out.println(
                                "TPL (load, devi) = (" + tplSol.getNetworkLoad() + ", " + tplSol.getDeviationSum() +
                                        ")");
                        System.out.println("TPL runtime: " + tplSolver.getLastRuntime());
                    } else {
                        deviationWriterTpl.append('i');
                        loadWriterTpl.append('i');
                    }

                    if (loadConstraint != X_END) {
                        deviationWriterTpl.append(',');
                        loadWriterTpl.append(',');
                    }

                    if (stFeasible) {
                        deviationWriterSt.append(String.valueOf(stSol.getDeviationSum()));
                        loadWriterSt.append(String.valueOf(stSol.getNetworkLoad()));

                        System.out.println(
                                "ST  (load, devi) = (" + stSol.getNetworkLoad() + ", " + stSol.getDeviationSum() + ")");
                    } else {
                        deviationWriterSt.append('i');
                        loadWriterSt.append('i');
                    }

                    if (loadConstraint != X_END) {
                        deviationWriterSt.append(',');
                        loadWriterSt.append(',');
                    }
                }

                deviationWriterTpl.append('\n');
                deviationWriterTpl.flush();
                deviationWriterSt.append('\n');
                deviationWriterSt.flush();
                loadWriterTpl.append('\n');
                loadWriterTpl.flush();
                loadWriterSt.append('\n');
                loadWriterSt.flush();
            }
        }
    }

    //@Test
    public void testSolve() {

        TestGraph graph = EvalTopoMockBuilder.getMockedTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 1);

        ArrayList<NprNfvTypes.Type> sfc = new ArrayList<>();
        sfc.add(NprNfvTypes.Type.TRANSCODER);

        ArrayList<NprNfvTypes.Type> sfc2 = new ArrayList<>();
        sfc2.add(NprNfvTypes.Type.FIREWALL);

        /*CustomDemandBuilder cdb = new CustomDemandBuilder();
        List<NprTraffic> demand = cdb.setIngress(graph.getById(DeviceId.deviceId("s10")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s7")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s5")))
                                     .setDemandValue(2.0)
                                     .setSfc(sfc)
                                     .createDemand()
                                     .setIngress(graph.getById(DeviceId.deviceId("s9")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s2")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s1")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s7")))
                                     .setDemandValue(6.0)
                                     .setSfc(sfc2)
                                     .createDemand()
                                     .generateDemand();*/
        for (int i = 0; i < 2; i++) {
            CustomDemandBuilder dg = new CustomDemandBuilder();
            dg.setRandomIngressAndEgress(2, graph.getVertexes()).setDemandValue(4).setRandomSfc(2).createDemand();
            dg.setRandomIngressAndEgress(2, graph.getVertexes()).setDemandValue(4).setRandomSfc(2).createDemand();
            dg.setRandomIngressAndEgress(2, graph.getVertexes()).setDemandValue(4).setRandomSfc(2).createDemand();
            List<NprTraffic> demand = dg.generateDemand();


            NfvPlacementRequest req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), demand, lw);

            INfvPlacementSolver refSolver = new RefSfcPlacementSolver(true, env, 1.0);

            //INfvPlacementSolver tplSolver = new TPLSfcPlacementSolver(OptimizationGoal
            //                                                                   .MIN_MAX_DELAYSUM_THEN_DEVIATION,
            //     true, env, 1.0);

            //INfvPlacementSolver stSolver = new STSfcPlacementSolver(OptimizationGoal
            //                                                                .MIN_MAX_DELAYSUM_THEN_DEVIATION, true,
            //       env, 1.0);

            //NfvPlacementSolution tplSol = tplSolver.solve(req);

            NfvPlacementSolution refSol = refSolver.solve(req);

            // NfvPlacementSolution stSol = stSolver.solve(req);

            if (true) {
                writeToCsvAndPlot(refSol, "/home/felix/Desktop/ba/code/eval/" + System.currentTimeMillis());
                //writeToCsvAndPlot(refSol, "/home/felix/Desktop/ba/code/eval/2" + System.currentTimeMillis());
                Assert.fail();
                return;
            }


        }


    }

    //@Test
    public void testMinimalNoSfc() {

        TestGraph graph = EvalTopoMockBuilder.getMockedTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 1);

        ArrayList<NprNfvTypes.Type> sfc = new ArrayList<>();
        sfc.add(NprNfvTypes.Type.TRANSCODER);

        CustomDemandBuilder cdb = new CustomDemandBuilder();
        List<NprTraffic> demand = cdb.setIngress(graph.getById(DeviceId.deviceId("s4")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s2")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s9")))
                                     .setDemandValue(10)
                                     .setSfc(new ArrayList<>())
                                     .createDemand()
                                     .setIngress(graph.getById(DeviceId.deviceId("s10")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s7")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s1")))
                                     .setDemandValue(2)
                                     .setSfc(sfc)
                                     .createDemand()
                                     .setIngress(graph.getById(DeviceId.deviceId("s10")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s7")))
                                     .addEgress(graph.getById(DeviceId.deviceId("s1")))
                                     .setDemandValue(6)
                                     .setSfc(sfc)
                                     .createDemand()
                                     .generateDemand();


        NfvPlacementRequest req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), demand, lw);

        INfvPlacementSolver solver = new RefSfcPlacementSolver(true, env, 1.0);

        NfvPlacementSolution sol = solver.solve(req);

        writeToCsvAndPlot(sol, "/home/felix/Desktop/ba/code/eval/" + System.currentTimeMillis());
    }

    @After
    public void tearDown() throws GRBException {
        env.dispose();
    }
}
