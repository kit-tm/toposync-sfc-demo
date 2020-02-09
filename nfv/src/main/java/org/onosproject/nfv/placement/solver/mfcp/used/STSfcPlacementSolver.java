package org.onosproject.nfv.placement.solver.mfcp.used;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.nfv.placement.solver.OptimizationGoal;
import thesiscode.common.nfv.traffic.NprTraffic;

import java.util.HashMap;
import java.util.Map;

/**
 * The implementation of the SFC-ST variant of the formulated ILP. Ensures that a single tree is constructed.
 */
public class STSfcPlacementSolver extends SfcPlacementSolver {
    private Map<NprTraffic, Map<TopologyEdge, GRBVar>> f; // f_t(i,j)
    private Map<NprTraffic, Map<TopologyVertex, GRBVar>> u; // u_t(v)


    /**
     * Creates a new STSfcPlacementSolver.
     *
     * @param goal    the goal to optimize
     * @param verbose whether verbose output is wanted or not
     * @param env     the environment (passing this as a parameter requires only one license check )
     * @param alpha   the weight factor for the VNF deployment cost
     */
    public STSfcPlacementSolver(OptimizationGoal goal, boolean verbose, GRBEnv env, double alpha) {
        super(goal, verbose, env, alpha, Integer.MAX_VALUE);
    }

    /**
     * Creates a new STSfcPlacementSolver.
     *
     * @param goal    the goal to optimize
     * @param verbose whether verbose output is wanted or not
     * @param env     the environment (passing this as a parameter requires only one license check )
     * @param alpha   the weight factor for the VNF deployment cost
     */
    public STSfcPlacementSolver(OptimizationGoal goal, boolean verbose, GRBEnv env, double alpha, int loadConstraint) {
        super(goal, verbose, env, alpha, loadConstraint);
    }

    @Override
    protected void addTreeVariables(GRBModel model) throws GRBException {
        // f variable
        f = new HashMap<>();
        for (NprTraffic flow : trafficSfc) {
            Map<TopologyEdge, GRBVar> edgeToVarMap = new HashMap<>();
            for (TopologyEdge edge : edges) {
                edgeToVarMap.put(edge, model.addVar(0.0, 1.0, 0.0, GRB.BINARY,
                                                    "f_" + flow.toString() + "_(" + edge.src().toString() + "," +
                                                            edge.dst().toString() + ")"));
            }
            f.put(flow, edgeToVarMap);
        }

        // connect f_t(i,j) to f_t^{(k,l)}(i,j)
        addFConnectionConstraints(model);

        // MTZ variables
        u = new HashMap<>();
        for (NprTraffic flow : trafficSfc) {
            Map<TopologyVertex, GRBVar> vertToVar = new HashMap<>();
            for (TopologyVertex vertex : nodes) {
                vertToVar.put(vertex, model.addVar(0.0,
                                                   nodes.size() - 1, 0.0, GRB.INTEGER,
                                                   "u_" + flow.toString() + "_vert=" + vertex.toString()));
            }
            u.put(flow, vertToVar);
        }
    }

    private void addFConnectionConstraints(GRBModel model) throws GRBException {
        // f_t^{(k,l)}(i,j) <= f_t(i,j)
        int flowCnt = 0;
        for (NprTraffic flow : trafficSfc) {
            for (int j = 0; j < fForLogical.get(flowCnt).size(); j++) {
                for (TopologyEdge edge : edges) {
                    model.addConstr(fForLogical.get(flowCnt).get(j).get(edge), GRB.LESS_EQUAL, f.get(flow).get(edge),
                                    "f_t^{(k," + "l)}(i,j)<=f_t(i,j)_" + flow.toString() + "_" + j + "_(" +
                                            edge.src().toString() + "," + edge.dst().toString() + ")");
                }
            }
            flowCnt++;
        }

        // f_t(i,j) <= sum of f_t^{(k,l)}(i,j)
        flowCnt = 0;
        for (NprTraffic flow : trafficSfc) {
            GRBLinExpr sum = new GRBLinExpr();
            for (TopologyEdge edge : edges) {
                for (int j = 0; j < fForLogical.get(flowCnt).size(); j++) {
                    sum.addTerm(1.0, fForLogical.get(flowCnt).get(j).get(edge));
                }
                model.addConstr(f.get(flow).get(edge), GRB.LESS_EQUAL, sum,
                                "f_t(i,j)<=sumOf-f_t^{(k,l)}(i,j)_" + flow.toString() + "_(" + edge.src().toString() +
                                        "," + edge.dst().toString() + ")");
            }
            flowCnt++;
        }
    }

    @Override
    protected void addMTZConstraints(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
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
            rhsWithoutMul.addTerm(-1.0, f.get(flow).get(edge));

            GRBLinExpr rhs = new GRBLinExpr();
            rhs.multAdd(nodes.size(), rhsWithoutMul);

            model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "lel");
        }
    }

    @Override
    protected void addInDegreeLessEqualOneConstraint(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        // sum over (i,j) in E over f_t(i,j) <= 1
        for (TopologyVertex node : nodes) {
            GRBLinExpr sum = new GRBLinExpr();
            for (TopologyEdge edge : edges) {
                if (edge.dst().equals(node)) {
                    sum.addTerm(1.0, f.get(flow).get(edge));
                }
            }
            model.addConstr(sum, GRB.LESS_EQUAL, 1.0, "");
        }
    }
}
