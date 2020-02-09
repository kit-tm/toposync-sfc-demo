package org.onosproject.nfv.placement.solver.mfcp.legacy;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ILinkWeigher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleMfcpSolver extends AbstractNfvIlpPlacementSolver {
    private Logger log = LoggerFactory.getLogger(SimpleMfcpSolver.class);

    private Set<TopologyVertex> nodes;
    private Set<TopologyEdge> edges;
    private ILinkWeigher linkWeigher;
    private List<NprTraffic> traffic;
    private NfvPlacementRequest req;

    private GRBVar[] utilization; // edge -> edge utilization
    private GRBVar[][] fractions; // flow -> edge -> edge fraction
    private GRBVar maxUtilization; // overall maximum edge utilization

    private GRBVar left[][]; // flow -> edge -> how many dsts left

    private OptimizationGoal goal;


    public SimpleMfcpSolver(OptimizationGoal goal, boolean verbose, GRBEnv env) {
        this.goal = goal;
        this.verbose = verbose;
        this.env = env;
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

        fractions = new GRBVar[traffic.size()][edges.size()];
        utilization = new GRBVar[edges.size()];
        left = new GRBVar[traffic.size()][edges.size()];
    }

    @Override
    protected void addVariables(GRBModel model) throws GRBException {
        int trafficCnt = 0;

        maxUtilization = model.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.CONTINUOUS, "max_util");

        for (NprTraffic trafficFlow : traffic) {
            int edgeCnt = 0;
            for (TopologyEdge edge : edges) {
                String name = "frac_" + trafficCnt + "_" + edge.src().deviceId().toString() + "->" +
                        edge.dst().deviceId().toString();
                fractions[trafficCnt][edgeCnt] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name);


                name = "left_" + trafficCnt + "_" + edge.src().deviceId().toString() + "->" +
                        edge.dst().deviceId().toString();

                left[trafficCnt][edgeCnt] = model.addVar(0.0, trafficFlow.getEgressNodes()
                                                                         .size(), 0.0, GRB.INTEGER, name);

                edgeCnt++;
            }
            trafficCnt++;
        }

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
            case REDUCTION_THEN_BALANCE:
                objExpr.addTerm(1.0, maxUtilization);
                model.setObjectiveN(objExpr, 1, 1, 1, 0, 0, "minMaxUtil");
                reduct = new GRBLinExpr();
                for (int i = 0; i < edges.size(); i++) {
                    reduct.addTerm(1.0, utilization[i]);
                }
                model.setObjectiveN(reduct, 0, 0, 1, 0, 0, "reduct");
                break;
            default:
                throw new IllegalStateException("Optimization goal not yet implemented in this solver.");
        }

    }

    @Override
    protected void addConstraints(GRBModel model) throws GRBException {
        /*
        for iterating over traffic and edges to access fractions[][]
         */
        int trafficCnt = 0;
        int edgeCnt = 0;

        // to calculcate max utilization
        model.addGenConstrMax(maxUtilization, utilization, 0, "constr_max_util");

        // (3.3) connection between f and l
        trafficCnt = 0;
        for (NprTraffic trafficFlow : traffic) {
            edgeCnt = 0;
            for (TopologyEdge edge : edges) {
                GRBLinExpr leftExpr = new GRBLinExpr();
                leftExpr.addTerm(1.0, left[trafficCnt][edgeCnt]);
                GRBLinExpr fracExpr = new GRBLinExpr();
                fracExpr.addTerm(trafficFlow.getEgressNodes().size(), fractions[trafficCnt][edgeCnt]);
                GRBLinExpr fracOneExpr = new GRBLinExpr();
                fracOneExpr.addTerm(1.0, fractions[trafficCnt][edgeCnt]);
                String name = "constr_left<=|D|*frac_" + trafficCnt + "_" + edge.src().deviceId().toString() + "->" +
                        edge.dst().deviceId().toString();
                model.addConstr(leftExpr, GRB.LESS_EQUAL, fracExpr, name);
                name = "frac<=constr_left_" + trafficCnt + "_" + edge.src().deviceId().toString() + "->" +
                        edge.dst().deviceId().toString();
                model.addConstr(fracOneExpr, GRB.LESS_EQUAL, leftExpr, name);
                edgeCnt++;
            }
            trafficCnt++;
        }

        // (3.4) link capacity not exceeded
        edgeCnt = 0;
        for (TopologyEdge edge : edges) {
            trafficCnt = 0;
            GRBLinExpr expr = new GRBLinExpr();
            for (NprTraffic trafficFlow : traffic) {
                expr.addTerm(trafficFlow.getDemand(), fractions[trafficCnt][edgeCnt]);
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

        // (3.5) source flow conservation
        trafficCnt = 0;
        for (NprTraffic trafficFlow : traffic) {
            edgeCnt = 0;

            GRBLinExpr minusExprLeft = new GRBLinExpr();
            GRBLinExpr fromIngressLeft = new GRBLinExpr();
            GRBLinExpr toIngressLeft = new GRBLinExpr();
            for (TopologyEdge edge : edges) {
                if (edge.src().equals(trafficFlow.getIngressNode())) {
                    // from ingress
                    fromIngressLeft.addTerm(1.0, left[trafficCnt][edgeCnt]);
                } else if (edge.dst().equals(trafficFlow.getIngressNode())) {
                    // to ingress
                    toIngressLeft.addTerm(1.0, left[trafficCnt][edgeCnt]);
                }
                edgeCnt++;
            }

            minusExprLeft.add(fromIngressLeft);
            minusExprLeft.multAdd(-1.0, toIngressLeft);
            String name =
                    "constr_src_cons_left_" + trafficCnt + "_" + trafficFlow.getIngressNode().deviceId().toString();
            model.addConstr(minusExprLeft, GRB.EQUAL, trafficFlow.getEgressNodes().size(), name);
            trafficCnt++;
        }

        // (3.6) destination flow conservation
        trafficCnt = 0;
        for (NprTraffic trafficFlow : traffic) {
            Set<TopologyVertex> destinations = trafficFlow.getEgressNodes();
            for (TopologyVertex dst : destinations) {
                edgeCnt = 0;

                GRBLinExpr minusExprLeft = new GRBLinExpr();
                GRBLinExpr fromDstLeft = new GRBLinExpr();
                GRBLinExpr toDstLeft = new GRBLinExpr();

                for (TopologyEdge edge : edges) {
                    if (edge.dst().equals(dst)) {
                        // to destination
                        toDstLeft.addTerm(1.0, left[trafficCnt][edgeCnt]);
                    } else if (edge.src().equals(dst)) {
                        // from destination
                        fromDstLeft.addTerm(1.0, left[trafficCnt][edgeCnt]);
                    }
                    edgeCnt++;
                }

                minusExprLeft.add(toDstLeft);
                minusExprLeft.multAdd(-1.0, fromDstLeft);
                String name = "constr_dst_cons_left" + trafficCnt + "_" + dst.deviceId().toString();
                model.addConstr(minusExprLeft, GRB.EQUAL, 1.0, name);

            }

            trafficCnt++;
        }

        // (3.7) transit node flow conservation
        trafficCnt = 0;
        for (NprTraffic trafficFlow : traffic) {
            for (TopologyVertex vert : nodes) {
                // source node is no transit node
                if (trafficFlow.getIngressNode().equals(vert)) {
                    continue;
                }
                // destination nodes are no transit nodes
                if (trafficFlow.getEgressNodes().contains(vert)) {
                    continue;
                }

                GRBLinExpr toVertLeft = new GRBLinExpr();
                GRBLinExpr fromVertLeft = new GRBLinExpr();

                edgeCnt = 0;
                for (TopologyEdge edge : edges) {
                    if (edge.dst().equals(vert)) {
                        toVertLeft.addTerm(1.0, left[trafficCnt][edgeCnt]);
                    } else if (edge.src().equals(vert)) {
                        fromVertLeft.addTerm(1.0, left[trafficCnt][edgeCnt]);
                    }
                    edgeCnt++;
                }

                String name = "constr_dupl_" + trafficCnt + "_" + vert.deviceId().toString();
                model.addConstr(fromVertLeft, GRB.EQUAL, toVertLeft, name);

            }

            trafficCnt++;
        }
    }

    @Override
    protected NfvPlacementSolution extractSolution(GRBModel model) throws GRBException {
        int trafficCnt = 0;
        int edgeCnt = 0;
        int vertCnt = 0;

        print("\nThis is the solution.");
        print("value: " + model.get(GRB.DoubleAttr.ObjVal));


        double fraction;
        double trafficLeft;
        Map<NprTraffic, Set<TopologyEdge>> solutionEdges = new HashMap<>();
        for (NprTraffic trafficFlow : traffic) {
            StringBuilder destinationString = new StringBuilder("{");
            for (TopologyVertex dst : trafficFlow.getEgressNodes()) {
                destinationString.append(dst.deviceId().toString()).append(", ");
            }
            destinationString.append("}");
            print("traffic from " + trafficFlow.getIngressNode().deviceId().toString() + " to " + destinationString +
                          " with " + "demand" + " " + trafficFlow.getDemand() + ":");

            print("  fractions:");


            edgeCnt = 0;
            Set<TopologyEdge> edgesForTraffic = new HashSet<>();

            for (TopologyEdge edge : edges) {
                fraction = fractions[trafficCnt][edgeCnt].get(GRB.DoubleAttr.X);

                if (fraction != 0) {
                    print("    frac_" + trafficCnt + "_" + edge.src().deviceId().toString() + "->" +
                                  edge.dst().deviceId().toString() + "=" + fraction);


                    edgesForTraffic.add(edge);
                }
                edgeCnt++;
            }

            solutionEdges.put(trafficFlow, edgesForTraffic);

            print("  left:");


            edgeCnt = 0;
            for (TopologyEdge edge : edges) {
                trafficLeft = left[trafficCnt][edgeCnt].get(GRB.DoubleAttr.X);

                if (trafficLeft != 0) {
                    print("    left_" + trafficCnt + "_" + edge.src().deviceId().toString() + "->" +
                                  edge.dst().deviceId().toString() + "=" + trafficLeft);
                }
                edgeCnt++;
            }

            trafficCnt++;
        }


        // TODO
        return null;
    }


}
