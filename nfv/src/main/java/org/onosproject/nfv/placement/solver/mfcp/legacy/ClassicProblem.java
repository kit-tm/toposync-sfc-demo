package org.onosproject.nfv.placement.solver.mfcp.legacy;

import gurobi.GRB;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ILinkWeigher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassicProblem extends AbstractNfvIlpPlacementSolver {
    private Logger log = LoggerFactory.getLogger(SimpleMfcpSolver.class);

    private Set<TopologyVertex> nodes;
    private Set<TopologyEdge> edges;
    private ILinkWeigher linkWeigher;
    private List<NprTraffic> traffic;
    private NfvPlacementRequest req;

    private GRBVar[] utilization; // edge -> edge utilization
    private GRBVar maxUtilization; // overall maximum edge utilization

    private Map<NprTraffic, Map<TopologyEdge, GRBVar>> f; // traffic -> edge -> {0,1}

    private OptimizationGoal goal;

    public ClassicProblem(OptimizationGoal goal, boolean verbose) {
        this.goal = goal;
        this.verbose = verbose;
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

        utilization = new GRBVar[edges.size()];
    }

    @Override
    protected void addVariables(GRBModel model) throws GRBException {
        int trafficCnt = 0;
        f = new HashMap<>();
        trafficCnt = 0;
        for (NprTraffic flow : traffic) {
            Map<TopologyEdge, GRBVar> edgeToVar = new HashMap<>();
            for (TopologyEdge edge : edges) {
                String name = new StringBuilder().append("f_")
                                                 .append(trafficCnt)
                                                 .append('_')
                                                 .append(edge.src().deviceId().toString())
                                                 .append("->")
                                                 .append(edge.dst().deviceId().toString())
                                                 .toString();
                edgeToVar.put(edge, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name));
            }
            f.put(flow, edgeToVar);
            trafficCnt++;
        }

        maxUtilization = model.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, "max_util");

        for (int i = 0; i < edges.size(); i++) {
            utilization[i] = model.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, "util_" + i);
        }
    }

    @Override
    protected void addObjective(GRBModel model) throws GRBException {
        // minimize max utilization -> simple load balancing objective
        GRBLinExpr objExpr = new GRBLinExpr();
        switch (goal) {
            case LOAD_BALANCING:
                objExpr.addTerm(1.0, maxUtilization);
                model.setObjective(objExpr, GRB.MINIMIZE);
                break;
            case BALANCE_THEN_REDUCTION:
                objExpr.addTerm(1.0, maxUtilization);
                model.setObjectiveN(objExpr, 0, 0, 1, 0, 0, "minMaxUtil");
                GRBLinExpr reduct = new GRBLinExpr();
                for (int i = 0; i < edges.size(); i++) {
                    reduct.addTerm(1.0, utilization[i]);
                }
                model.setObjectiveN(reduct, 1, 1, 1, 0, 0, "reduct");
                break;
            case LOAD_REDUCTION:
                for (int i = 0; i < edges.size(); i++) {
                    objExpr.addTerm(1.0, utilization[i]);
                }
                model.setObjective(objExpr, GRB.MINIMIZE);
                break;
            default:
                throw new IllegalStateException("Optimization goal not yet implemented in this solver.");
        }

    }

    @Override
    protected void addConstraints(GRBModel model) throws GRBException {
        int trafficCnt = 0;
        int edgeCnt = 0;

        // to calculcate max utilization
        model.addGenConstrMax(maxUtilization, utilization, 0, "constr_max_util");

        // (4.9) link capacity not exceeded
        edgeCnt = 0;
        for (TopologyEdge edge : edges) {
            trafficCnt = 0;
            GRBLinExpr expr = new GRBLinExpr();
            for (NprTraffic flow : traffic) {
                expr.addTerm(flow.getDemand(), f.get(flow).get(edge));
                trafficCnt++;
            }
            String name =
                    "constr_cap_not_exc_" + edge.src().deviceId().toString() + "->" + edge.dst().deviceId().toString();
            model.addConstr(expr, GRB.LESS_EQUAL, linkWeigher.getBandwidth(edge), name);

            // to calculcate utilization
            name = "constr_fill_util_" + edge.src().deviceId().toString() + "->" + edge.dst().deviceId().toString();
            model.addConstr(utilization[edgeCnt], GRB.EQUAL, expr, name);

            edgeCnt++;
        }

        // (4.10) source flow conservation
        trafficCnt = 0;
        for (NprTraffic flow : traffic) {
            GRBLinExpr fromIngress = new GRBLinExpr();
            GRBLinExpr toIngress = new GRBLinExpr();
            for (TopologyEdge edge : edges) {
                if (edge.src().equals(flow.getIngressNode())) {
                    fromIngress.addTerm(1.0, f.get(flow).get(edge));
                } else if (edge.dst().equals(flow.getIngressNode())) {
                    toIngress.addTerm(1.0, f.get(flow).get(edge));
                }
            }

            GRBLinExpr minusExpr = new GRBLinExpr();
            minusExpr.add(fromIngress);
            minusExpr.multAdd(-1.0, toIngress);
            String name = new StringBuilder().append("constr_src_cons_")
                                             .append(trafficCnt)
                                             .append('_')
                                             .append(flow.getIngressNode().deviceId().toString())
                                             .toString();
            model.addConstr(minusExpr, GRB.EQUAL, 1.0, name);

            trafficCnt++;
        }

        // (4.11) destination flow conservation
        trafficCnt = 0;
        for (NprTraffic flow : traffic) {
            for (TopologyVertex dst : flow.getEgressNodes()) {
                GRBLinExpr fromDst = new GRBLinExpr();
                GRBLinExpr toDst = new GRBLinExpr();
                for (TopologyEdge edge : edges) {
                    if (edge.src().equals(dst)) {
                        fromDst.addTerm(1.0, f.get(flow).get(edge));
                    } else if (edge.dst().equals(dst)) {
                        toDst.addTerm(1.0, f.get(flow).get(edge));
                    }
                }

                GRBLinExpr minusExpr = new GRBLinExpr();
                minusExpr.add(toDst);
                minusExpr.multAdd(-1.0, fromDst);
                String name = new StringBuilder().append("constr_dst_cons_")
                                                 .append(trafficCnt)
                                                 .append('_')
                                                 .append(dst.deviceId().toString())
                                                 .toString();
                model.addConstr(minusExpr, GRB.EQUAL, 1.0, name);
            }
            trafficCnt++;
        }

        // (4.12) transit node flow conservation
        trafficCnt = 0;
        for (NprTraffic flow : traffic) {

            for (TopologyVertex vert : nodes) {
                // source node is no transit node
                if (flow.getIngressNode().equals(vert)) {
                    continue;
                }
                // current destination node is no transit node, rest is
                if (flow.getEgressNodes().contains(vert)) {
                    continue;
                }

                GRBLinExpr toVert = new GRBLinExpr();
                GRBLinExpr fromVert = new GRBLinExpr();

                for (TopologyEdge edge : edges) {
                    if (edge.dst().equals(vert)) {
                        toVert.addTerm(1.0, f.get(flow).get(edge));
                    } else if (edge.src().equals(vert)) {
                        fromVert.addTerm(1.0, f.get(flow).get(edge));
                    }
                }

                String name = "constr_dupl_" + trafficCnt + "vert=" + vert.deviceId().toString();
                model.addConstr(fromVert, GRB.EQUAL, toVert, name);


            }
            trafficCnt++;
        }
    }

    @Override
    protected NfvPlacementSolution extractSolution(GRBModel model) throws GRBException {
        int trafficCnt = 0;
        int edgeCnt = 0;
        int vertCnt = 0;

        if (verbose) {
            System.out.println("\nThis is the solution.");
            System.out.println("value: " + model.get(GRB.DoubleAttr.ObjVal));
        }

        double fraction;
        double trafficLeft;
        Map<NprTraffic, Set<TopologyEdge>> solutionEdges = new HashMap<>();
        for (NprTraffic trafficFlow : traffic) {
            if (verbose) {
                StringBuilder destinationString = new StringBuilder("{");
                for (TopologyVertex dst : trafficFlow.getEgressNodes()) {
                    destinationString.append(dst.deviceId().toString()).append(", ");
                }
                destinationString.append("}");
                System.out.println("traffic from " + trafficFlow.getIngressNode().deviceId().toString() + " to " +
                                           destinationString + " with " + "demand" + " " + trafficFlow.getDemand() +
                                           ":");

                System.out.println("  fractions:");
            }


            edgeCnt = 0;
            Set<TopologyEdge> edgesForTraffic = new HashSet<>();

            for (TopologyEdge edge : edges) {
                fraction = f.get(trafficFlow).get(edge).get(GRB.DoubleAttr.X);

                if (fraction != 0) {
                    if (verbose) {
                        System.out.println("    f_" + trafficCnt + "_" + edge.src().deviceId().toString() + "->" +
                                                   edge.dst().deviceId().toString() + "=" + fraction);
                    }

                    edgesForTraffic.add(edge);
                }
                edgeCnt++;
            }

            solutionEdges.put(trafficFlow, edgesForTraffic);
            trafficCnt++;
        }

        // TODO
        return null;
    }

}
