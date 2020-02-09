package org.onosproject.nfv.placement.solver.mfcp.used;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.nfv.placement.solver.AbstractNfvIlpPlacementSolver;
import org.onosproject.nfv.placement.solver.NfvPlacementRequest;
import org.onosproject.nfv.placement.solver.NfvPlacementSolution;
import org.onosproject.nfv.placement.solver.OptimizationGoal;
import org.onosproject.nfv.placement.solver.mfcp.legacy.SimpleMfcpSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ILinkWeigher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the ILP without SFC.
 */
public class NoSfcPlacementSolver extends AbstractNfvIlpPlacementSolver {
    private Logger log = LoggerFactory.getLogger(SimpleMfcpSolver.class);

    private Set<TopologyVertex> nodes;
    private Set<TopologyEdge> edges;
    private ILinkWeigher linkWeigher;
    private List<NprTraffic> traffic;
    private NfvPlacementRequest req;

    private Map<NprTraffic, Map<TopologyVertex, GRBVar>> u; // u_t(v) for MTZ constraints

    // flow -> edge -> {0,1}
    private Map<NprTraffic, Map<TopologyEdge, GRBVar>> lEdgeUsedAtAll; // l_t(i,j)

    // flow -> destination -> edge ->  {0,1}
    private Map<NprTraffic, Map<TopologyVertex, Map<TopologyEdge, GRBVar>>> lEdgeUsedForDestination; // l_t^{(s,d)}(i,j)
    private Map<NprTraffic, Map<TopologyVertex, GRBVar>> delay;
    private Map<NprTraffic, GRBVar> minDelayPerFlow;
    private Map<NprTraffic, GRBVar> maxDelayPerFlow;

    private OptimizationGoal goal;

    /**
     * Constructs a new solver instance. This instance can be used for solving several {@link NfvPlacementRequest}s.
     *
     * @param goal    the {@link OptimizationGoal} for this solver
     * @param verbose whether to verbosely output the solution in the console
     * @param env     the {@link gurobi.GRBEnv} this solver uses
     */
    public NoSfcPlacementSolver(OptimizationGoal goal, boolean verbose, GRBEnv env) {
        this.goal = goal;
        super.verbose = verbose;
        super.env = env;
    }


    @Override
    protected void init(NfvPlacementRequest req, GRBModel model) {
        this.req = req;
        edges = req.getEdges();
        nodes = req.getVertices();
        linkWeigher = req.getLinkWeigher();
        traffic = req.getTraffic();

        for (NprTraffic trafficFlow : traffic) {
            if (trafficFlow.getSfc().size() > 0) {
                throw new IllegalArgumentException("no SFC is accepted by this solver");
            }
        }
    }

    @Override
    protected void addVariables(GRBModel model) throws GRBException {
        addLEdgeUsedForDestination(model);
        addLEdgeUsedAtAll(model);
        addU(model);
        addDelay(model);
        addMinDelayPerFlow(model);
        addMaxDelayPerFlow(model);
    }

    private void addMaxDelayPerFlow(GRBModel model) throws GRBException {
        maxDelayPerFlow = new HashMap<>();
        int flowCnt = 0;
        for (NprTraffic flow : traffic) {
            maxDelayPerFlow.put(flow, model.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.INTEGER, "maxDelay_" + flowCnt));
            flowCnt++;
        }

    }

    private void addMinDelayPerFlow(GRBModel model) throws GRBException {
        minDelayPerFlow = new HashMap<>();
        int flowCnt = 0;
        for (NprTraffic flow : traffic) {
            minDelayPerFlow.put(flow, model.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.INTEGER, "minDelay_" + flowCnt));
            flowCnt++;
        }
    }

    private void addDelay(GRBModel model) throws GRBException {
        delay = new HashMap<>();
        int trafficCnt = 0;
        for (NprTraffic flow : traffic) {
            Map<TopologyVertex, GRBVar> dstToVar = new HashMap<>();
            for (TopologyVertex dst : flow.getEgressNodes()) {
                String name = "delay_" + trafficCnt + "_dst=" + dst.toString();
                dstToVar.put(dst, model.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.INTEGER, name));
            }
            delay.put(flow, dstToVar);
            trafficCnt++;
        }
    }

    private void addU(GRBModel model) throws GRBException {
        u = new HashMap<>();
        int flowCnt = 0;
        for (NprTraffic flow : traffic) {
            Map<TopologyVertex, GRBVar> vertToVar = new HashMap<>();
            for (TopologyVertex vertex : nodes) {
                vertToVar.put(vertex, model.addVar(0.0,
                                                   nodes.size() - 1, 0.0, GRB.INTEGER,
                                                   "u_" + flowCnt + "_vert=" + vertex.toString()));
            }
            u.put(flow, vertToVar);
            flowCnt++;
        }
    }

    private void addLEdgeUsedForDestination(GRBModel model) throws GRBException {
        lEdgeUsedForDestination = new HashMap<>();
        int trafficCnt = 0;
        for (NprTraffic flow : traffic) {
            Map<TopologyVertex, Map<TopologyEdge, GRBVar>> dstToEdge = new HashMap<>();
            for (TopologyVertex dst : flow.getEgressNodes()) { // logical edge s->dst
                Map<TopologyEdge, GRBVar> edgeToVar = new HashMap<>();
                for (TopologyEdge edge : edges) {
                    String name = "l_" + trafficCnt + '_' + dst.deviceId().toString() + '_' +
                            edge.src().deviceId().toString() + "->" + edge.dst().deviceId().toString();
                    edgeToVar.put(edge, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name));
                }
                dstToEdge.put(dst, edgeToVar);
            }
            lEdgeUsedForDestination.put(flow, dstToEdge);
            trafficCnt++;
        }
    }

    public GRBVar getIsEdgeUsedAtAllForFlow(NprTraffic flow, TopologyEdge edge) {
        return lEdgeUsedAtAll.get(flow).get(edge);
    }

    public GRBVar getMaxDelayForFlow(NprTraffic flow) {
        return maxDelayPerFlow.get(flow);
    }

    public GRBVar getMinDelayForFlow(NprTraffic flow) {
        return minDelayPerFlow.get(flow);
    }

    public GRBVar getDelayForFlowAndDestination(NprTraffic flow, TopologyVertex dst) {
        return delay.get(flow).get(dst);
    }

    public void addLEdgeUsedAtAll(GRBModel model) throws GRBException {
        lEdgeUsedAtAll = new HashMap<>();
        int trafficCnt = 0;
        for (NprTraffic flow : traffic) {
            Map<TopologyEdge, GRBVar> edgeToVar = new HashMap<>();
            for (TopologyEdge edge : edges) {
                String name = "lAtAll_" + trafficCnt + '_' + edge.src().deviceId().toString() + "->" +
                        edge.dst().deviceId().toString();
                edgeToVar.put(edge, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name));
            }
            lEdgeUsedAtAll.put(flow, edgeToVar);
            trafficCnt++;
        }
    }

    @Override
    protected void addObjective(GRBModel model) throws GRBException {
        GRBLinExpr objExpr = new GRBLinExpr();


        GRBLinExpr deviations = new GRBLinExpr();
        for (NprTraffic flow : traffic) {
            deviations.addTerm(1.0, maxDelayPerFlow.get(flow));
            deviations.addTerm(-1.0, minDelayPerFlow.get(flow));
        }

        switch (goal) {
            case DELAY_REDUCTION_PER_DST_SUM:
                for (NprTraffic flow : traffic) {
                    for (TopologyVertex dst : flow.getEgressNodes()) {
                        objExpr.addTerm(1.0, delay.get(flow).get(dst));
                    }
                }
                model.setObjective(objExpr, GRB.MINIMIZE);
                break;
            case DELAY_REDUCTION_PER_DST_SUM_MULTI:
                for (NprTraffic flow : traffic) {
                    for (TopologyVertex dst : flow.getEgressNodes()) {
                        objExpr.addTerm(1.0, delay.get(flow).get(dst));
                    }
                }
                model.setObjectiveN(objExpr, 0, 1, 1, 0, 0, "delaySum");

                model.setObjectiveN(deviations, 1, 0, 1, 0, 0, "deviationSum");
                break;
            case MIN_MAX_DELAYSUM_THEN_DEVIATION:
                for (NprTraffic flow : traffic) {
                    objExpr.addTerm(1.0, maxDelayPerFlow.get(flow));
                }
                model.setObjectiveN(objExpr, 1, 1, 1, 0, 0, "maxDelayPerFlowSum");

                model.setObjectiveN(deviations, 2, 0, 1, 0, 0, "deviationSum");
                break;
            default:
                throw new IllegalStateException("Optimization goal not yet implemented in this solver.");
        }
    }

    @Override
    protected void addConstraints(GRBModel model) throws GRBException {

        // ensure tree construction
        addInDegreeLessEqualOneConstraint(model);
        addMTZConstraints(model);


        // delay constraints
        addDelayConstraints(model);

        // connect decision variables
        addDecisionVariableConnectionConstraints(model);

        // link capacity
        addLinkCapacityConstraint(model);

        // flow conservations
        addSourceFlowConservation(model);
        addDestinationFlowConservation(model);
        addTransitFlowConservation(model);

    }

    public void addInDegreeLessEqualOneConstraint(GRBModel model) throws GRBException {
        // tree is build (in-degree <= 1)
        for (NprTraffic flow : traffic) {
            for (TopologyVertex vert : nodes) {
                GRBLinExpr lSum = new GRBLinExpr();
                for (TopologyEdge edge : edges) {
                    if (edge.dst().equals(vert)) {
                        lSum.addTerm(1.0, lEdgeUsedAtAll.get(flow).get(edge));
                    }
                }
                model.addConstr(lSum, GRB.LESS_EQUAL, 1, "");
            }
        }
    }

    public void addMTZConstraints(GRBModel model) throws GRBException {
        // MTZ -> avoid (disconnected) cycles
        for (NprTraffic flow : traffic) {
            for (TopologyVertex vertex : nodes) {
                if (vertex.equals(flow.getIngressNode())) {
                    model.addConstr(u.get(flow).get(flow.getIngressNode()), GRB.EQUAL, 0.0, "");

                } else {
                    model.addConstr(u.get(flow).get(vertex), GRB.LESS_EQUAL, nodes.size() - 1, "");
                    model.addConstr(u.get(flow).get(vertex), GRB.GREATER_EQUAL, 1.0, "");
                }

            }

            for (TopologyEdge edge : edges) {
                GRBLinExpr lhs = new GRBLinExpr();
                lhs.addTerm(1.0, u.get(flow).get(edge.src()));
                lhs.addTerm(-1.0, u.get(flow).get(edge.dst()));
                lhs.addConstant(1.0);

                GRBLinExpr rhsWithoutMul = new GRBLinExpr();
                rhsWithoutMul.addConstant(1.0);
                rhsWithoutMul.addTerm(-1.0, lEdgeUsedAtAll.get(flow).get(edge));

                GRBLinExpr rhs = new GRBLinExpr();
                rhs.multAdd(nodes.size(), rhsWithoutMul);

                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "lel");
            }
        }
    }

    public void addDelayConstraints(GRBModel model) throws GRBException {
        // delay stuff
        int flowCnt = 0;
        for (NprTraffic flow : traffic) {
            // delay per dst
            for (TopologyVertex dst : flow.getEgressNodes()) {
                GRBLinExpr sum = new GRBLinExpr();
                for (TopologyEdge edge : edges) {
                    sum.addTerm(linkWeigher.getDelay(edge), lEdgeUsedForDestination.get(flow).get(dst).get(edge));
                }
                String name = "constr_delay_" + flowCnt + "_dst=" + dst.toString();
                model.addConstr(delay.get(flow).get(dst), GRB.EQUAL, sum, name);
                // min max delay constr only work with this goal
                if (goal == OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION) {
                    model.addConstr(maxDelayPerFlow.get(flow), GRB.GREATER_EQUAL, delay.get(flow).get(dst), "");

                    model.addConstr(minDelayPerFlow.get(flow), GRB.LESS_EQUAL, delay.get(flow).get(dst), "");
                }

            }

            if (goal != OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION) {
                //max delay
                model.addGenConstrMax(maxDelayPerFlow.get(flow), delay.get(flow).values().toArray(new GRBVar[0]), 0.0,
                                      "constr_maxDelay" + flowCnt);


                // min delay
                model.addGenConstrMin(minDelayPerFlow.get(flow), delay.get(flow)
                                                                      .values()
                                                                      .toArray(new GRBVar[0]), Double.MAX_VALUE,
                                      "constr_minDelay" + flowCnt);
            }


            flowCnt++;
        }
    }

    public void addDecisionVariableConnectionConstraints(GRBModel model) throws GRBException {
        int flowCnt = 0;
        for (NprTraffic flow : traffic) {
            for (TopologyEdge edge : edges) {
                // l_t <= l_{t;d}
                for (TopologyVertex dst : flow.getEgressNodes()) {
                    GRBLinExpr lExpr = new GRBLinExpr();
                    lExpr.addTerm(1.0, lEdgeUsedForDestination.get(flow).get(dst).get(edge));
                    String name = "constr_l_{t;d}<=l_t" + flowCnt + '_' + edge.src().deviceId().toString() + "->" +
                            edge.dst().deviceId().toString();

                    model.addConstr(lExpr, GRB.LESS_EQUAL, lEdgeUsedAtAll.get(flow).get(edge), name);
                }

                // l <= sumL_d
                GRBLinExpr lSum = new GRBLinExpr();
                for (TopologyVertex dst : flow.getEgressNodes()) {
                    lSum.addTerm(1.0, lEdgeUsedForDestination.get(flow).get(dst).get(edge));
                }

                String name = "constr_l_{t;d}<=sum_l_t_" + flowCnt + '_' + edge.src().deviceId().toString() + "->" +
                        edge.dst().deviceId().toString();

                model.addConstr(lEdgeUsedAtAll.get(flow).get(edge), GRB.LESS_EQUAL, lSum, name);
            }
            flowCnt++;
        }
    }

    public void addLinkCapacityConstraint(GRBModel model) throws GRBException {
        // link capacity not exceeded
        for (TopologyEdge edge : edges) {
            GRBLinExpr expr = new GRBLinExpr();
            for (NprTraffic flow : traffic) {
                expr.addTerm(flow.getDemand(), lEdgeUsedAtAll.get(flow).get(edge));
            }
            String name =
                    "constr_cap_not_exc_" + edge.src().deviceId().toString() + "->" + edge.dst().deviceId().toString();
            model.addConstr(expr, GRB.LESS_EQUAL, linkWeigher.getBandwidth(edge), name);
        }
    }

    public void addSourceFlowConservation(GRBModel model) throws GRBException {
        //  source flow conservation
        int flowCnt = 0;
        for (NprTraffic flow : traffic) {
            for (TopologyVertex dst : flow.getEgressNodes()) {
                GRBLinExpr fromIngress = new GRBLinExpr();
                GRBLinExpr toIngress = new GRBLinExpr();
                for (TopologyEdge edge : edges) {
                    if (edge.src().equals(flow.getIngressNode())) {
                        fromIngress.addTerm(1.0, lEdgeUsedForDestination.get(flow).get(dst).get(edge));
                    } else if (edge.dst().equals(flow.getIngressNode())) {
                        toIngress.addTerm(1.0, lEdgeUsedForDestination.get(flow).get(dst).get(edge));
                    }
                }

                GRBLinExpr minusExpr = new GRBLinExpr();
                minusExpr.add(fromIngress);
                minusExpr.multAdd(-1.0, toIngress);
                String name = "constr_src_cons_" + flowCnt + '_' + dst.deviceId().toString();
                model.addConstr(minusExpr, GRB.EQUAL, 1.0, name);
            }
            flowCnt++;
        }
    }

    public void addDestinationFlowConservation(GRBModel model) throws GRBException {
        int flowCnt = 0;
        for (NprTraffic flow : traffic) {
            for (TopologyVertex dst : flow.getEgressNodes()) {
                GRBLinExpr fromDst = new GRBLinExpr();
                GRBLinExpr toDst = new GRBLinExpr();
                for (TopologyEdge edge : edges) {
                    if (edge.src().equals(dst)) {
                        fromDst.addTerm(1.0, lEdgeUsedForDestination.get(flow).get(dst).get(edge));
                    } else if (edge.dst().equals(dst)) {
                        toDst.addTerm(1.0, lEdgeUsedForDestination.get(flow).get(dst).get(edge));
                    }
                }

                GRBLinExpr minusExpr = new GRBLinExpr();
                minusExpr.add(toDst);
                minusExpr.multAdd(-1.0, fromDst);
                String name = "constr_dst_cons_" + flowCnt + '_' + dst.deviceId().toString();
                model.addConstr(minusExpr, GRB.EQUAL, 1.0, name);
            }
        }
    }

    public void addTransitFlowConservation(GRBModel model) throws GRBException {
        // transit node flow conservation
        int flowCnt = 0;
        for (NprTraffic flow : traffic) {
            for (TopologyVertex dst : flow.getEgressNodes()) {
                for (TopologyVertex vert : nodes) {
                    // source node is no transit node
                    if (flow.getIngressNode().equals(vert)) {
                        continue;
                    }
                    // current destination node is no transit node, rest is
                    if (vert.equals(dst)) {
                        continue;
                    }

                    GRBLinExpr toVert = new GRBLinExpr();
                    GRBLinExpr fromVert = new GRBLinExpr();

                    for (TopologyEdge edge : edges) {
                        if (edge.dst().equals(vert)) {
                            toVert.addTerm(1.0, lEdgeUsedForDestination.get(flow).get(dst).get(edge));
                        } else if (edge.src().equals(vert)) {
                            fromVert.addTerm(1.0, lEdgeUsedForDestination.get(flow).get(dst).get(edge));
                        }
                    }

                    String name = "constr_dupl_" + flowCnt + "dst=" + dst.deviceId().toString() + "vert=" +
                            vert.deviceId().toString();
                    model.addConstr(fromVert, GRB.EQUAL, toVert, name);

                }
            }
            flowCnt++;
        }
    }

    @Override
    protected NfvPlacementSolution extractSolution(GRBModel model) throws GRBException {
        print("\nThis is the solution.");
        print("value: " + model.get(GRB.DoubleAttr.ObjVal));

        double varValue;
        GRBVar var;
        int deviationSum = 0;
        int delaySum = 0;
        int networkLoad = 0;
        Map<NprTraffic, Set<TopologyEdge>> solutionEdges = new HashMap<>();
        Map<NprTraffic, Double> maxDelayMap = new HashMap<>();
        Map<NprTraffic, Double> deviationMap = new HashMap<>();
        for (NprTraffic flow : traffic) {
            print(flow.toString());

            for (TopologyVertex dst : flow.getEgressNodes()) {
                delaySum += Math.round(delay.get(flow).get(dst).get(GRB.DoubleAttr.X));
            }

            double minDelayOfFlow = Math.round(minDelayPerFlow.get(flow).get(GRB.DoubleAttr.X));
            double maxDelayOfFlow = Math.round(maxDelayPerFlow.get(flow).get(GRB.DoubleAttr.X));
            maxDelayMap.put(flow, maxDelayOfFlow);

            deviationSum -= minDelayOfFlow;
            deviationSum += maxDelayOfFlow;
            deviationMap.put(flow, (maxDelayOfFlow - minDelayOfFlow));

            print("  minDelay:" + minDelayOfFlow);
            print("  maxDelay:" + maxDelayOfFlow);

            for (TopologyVertex vert : flow.getEgressNodes()) {
                print("  delay for " + vert.toString() + "=" + delay.get(flow).get(vert).get(GRB.DoubleAttr.X));
            }


            Set<TopologyEdge> edgesForTraffic = new HashSet<>();

            print("  f:");
            for (TopologyEdge edge : edges) {
                var = lEdgeUsedAtAll.get(flow).get(edge);
                varValue = Math.round(var.get(GRB.DoubleAttr.X));
                if (varValue != 0) {
                    print("    " + var.get(GRB.StringAttr.VarName) + "=" + varValue);
                    edgesForTraffic.add(edge);
                    networkLoad += flow.getDemand() * varValue;
                }
            }
            solutionEdges.put(flow, edgesForTraffic);

            print("  l:");
            for (TopologyVertex dst : flow.getEgressNodes()) {
                for (TopologyEdge edge : edges) {
                    var = lEdgeUsedForDestination.get(flow).get(dst).get(edge);
                    varValue = Math.round(var.get(GRB.DoubleAttr.X));
                    if (varValue != 0) {
                        print("    " + var.get(GRB.StringAttr.VarName) + "=" + varValue);
                    }
                }
            }
        }

        return new NfvPlacementSolution(solutionEdges, new HashMap<>(), req, goal, model.get(GRB.DoubleAttr.ObjNVal), deviationSum, delaySum, networkLoad, deviationMap, maxDelayMap);
    }
}
