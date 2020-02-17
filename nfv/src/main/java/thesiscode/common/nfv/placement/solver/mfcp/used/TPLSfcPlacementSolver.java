package thesiscode.common.nfv.placement.solver.mfcp.used;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.nfv.placement.solver.OptimizationGoal;
import org.slf4j.Logger;
import thesiscode.common.nfv.traffic.NprTraffic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The implementation of the SFC-TPL variant of the formulated ILP. Ensures that a tree is constructed per logical edge.
 */
public class TPLSfcPlacementSolver extends SfcPlacementSolver {
    /*
     * MTZ variables
     */
    // flow -> logical edge -> node -> node number
    private List<List<Map<TopologyVertex, GRBVar>>> u;

    /**
     * Creates a new TPLSfcPlacementSolver.
     *
     * @param goal    the goal to optimize
     * @param verbose whether verbose output is wanted or not
     * @param env     the environment (passing this as a parameter requires only one license check )
     * @param alpha   the weight factor for the VNF deployment cost
     */
    public TPLSfcPlacementSolver(OptimizationGoal goal, boolean verbose, GRBEnv env, double alpha) {
        super(goal, verbose, env, alpha, Integer.MAX_VALUE);
    }

    /**
     * Creates a new TPLSfcPlacementSolver.
     *
     * @param goal    the goal to optimize
     * @param verbose whether verbose output is wanted or not
     * @param env     the environment (passing this as a parameter requires only one license check )
     * @param alpha   the weight factor for the VNF deployment cost
     */
    public TPLSfcPlacementSolver(OptimizationGoal goal, boolean verbose, GRBEnv env, double alpha, int loadConstraint) {
        super(goal, verbose, env, alpha, loadConstraint);
    }

    /**
     * Creates a new TPLSfcPlacementSolver.
     *
     * @param goal    the goal to optimize
     * @param verbose whether verbose output is wanted or not
     * @param env     the environment (passing this as a parameter requires only one license check )
     * @param alpha   the weight factor for the VNF deployment cost
     */
    public TPLSfcPlacementSolver(OptimizationGoal goal, boolean verbose, GRBEnv env, double alpha, Logger log) {
        super(goal, verbose, env, alpha, log);
    }


    @Override
    protected void addTreeVariables(GRBModel model) throws GRBException {
        u = new ArrayList<>();
        for (int i = 0; i < trafficSfc.size(); i++) {
            NprTraffic flow = trafficSfc.get(i);

            ArrayList<Map<TopologyVertex, GRBVar>> logicalEdgeToMap = new ArrayList<>();
            for (int j = 0; j <= flow.getSfc().size(); j++) {
                Map<TopologyVertex, GRBVar> vertToNumber = new HashMap<>();
                for (TopologyVertex vert : nodes) {
                    vertToNumber.put(vert, model.addVar(0.0,
                                                        nodes.size() - 1, 0.0, GRB.INTEGER,
                                                        "u_" + i + "_" + j + "_vert=" + vert.toString()));
                }
                logicalEdgeToMap.add(vertToNumber);
            }
            u.add(logicalEdgeToMap);
        }
    }

    @Override
    protected void addMTZConstraints(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        // MTZ
        for (int j = 0; j < u.get(flowIndex).size(); j++) {
            for (TopologyVertex vertex : nodes) {
                GRBLinExpr rhsSource = new GRBLinExpr(); // (|V|-1)*(1-p_t^k(v))

                GRBLinExpr rhsWithoutMult = new GRBLinExpr(); // (1-p_t^k(v))
                rhsWithoutMult.addTerm(-1.0, pPlacedForFlow.get(flowIndex).get(j).get(vertex));
                rhsWithoutMult.addConstant(1.0);

                rhsSource.multAdd(nodes.size() - 1, rhsWithoutMult);


                model.addConstr(u.get(flowIndex).get(j).get(vertex), GRB.LESS_EQUAL, rhsSource,
                                "u_t^{(k,l)}(v)<=(|V|-1)*(1-p_t^k(v))_" + flowIndex + "_" + j + "_vert=" +
                                        vertex.toString());


                model.addConstr(u.get(flowIndex).get(j).get(vertex), GRB.LESS_EQUAL, nodes.size() - 1, "eho");
                model.addConstr(u.get(flowIndex).get(j).get(vertex), GRB.GREATER_EQUAL, rhsWithoutMult, "oha");

            }

            for (TopologyEdge edge : edges) {
                GRBLinExpr lhs = new GRBLinExpr();
                lhs.addTerm(1.0, u.get(flowIndex).get(j).get(edge.src()));
                lhs.addTerm(-1.0, u.get(flowIndex).get(j).get(edge.dst()));
                lhs.addConstant(1.0);

                GRBLinExpr rhsWithoutMul = new GRBLinExpr();
                rhsWithoutMul.addConstant(1.0);
                rhsWithoutMul.addTerm(-1.0, fForLogical.get(flowIndex).get(j).get(edge));

                GRBLinExpr rhs = new GRBLinExpr();
                rhs.multAdd(nodes.size(), rhsWithoutMul);

                model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "");
            }
        }
    }

    @Override
    protected void addInDegreeLessEqualOneConstraint(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        // in-degree <= 1
        for (TopologyVertex vert : nodes) {
            // logical edges
            for (int j = 0; j <= flow.getSfc().size(); j++) {
                GRBLinExpr sum = new GRBLinExpr();
                for (TopologyEdge edge : edges) {
                    if (edge.dst().equals(vert)) {
                        sum.addTerm(1.0, fForLogical.get(flowIndex).get(j).get(edge));
                    }
                }
                model.addConstr(sum, GRB.LESS_EQUAL, 1.0, "");
            }
        }
    }

}
