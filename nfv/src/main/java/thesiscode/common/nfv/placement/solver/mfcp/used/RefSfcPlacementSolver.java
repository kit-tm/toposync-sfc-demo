package thesiscode.common.nfv.placement.solver.mfcp.used;

import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import org.slf4j.Logger;
import thesiscode.common.nfv.placement.solver.NfvPlacementSolution;
import thesiscode.common.nfv.placement.solver.OptimizationGoal;
import thesiscode.common.nfv.traffic.NprTraffic;

public class RefSfcPlacementSolver extends SfcPlacementSolver {

    /**
     * Creates a new RefSfcPlacementSolver.
     *
     * @param verbose whether verbose output is wanted or not
     * @param env     the environment (passing this as a parameter requires only one license check )
     * @param alpha   the weight factor for the VNF deployment cost
     */
    public RefSfcPlacementSolver(boolean verbose, GRBEnv env, double alpha) {
        super(OptimizationGoal.SPT, verbose, env, alpha, Integer.MAX_VALUE);
    }

    /**
     * Creates a new RefSfcPlacementSolver.
     *
     * @param verbose whether verbose output is wanted or not
     * @param env     the environment (passing this as a parameter requires only one license check )
     * @param alpha   the weight factor for the VNF deployment cost
     */
    public RefSfcPlacementSolver(boolean verbose, GRBEnv env, double alpha, int loadConstraint) {
        super(OptimizationGoal.SPT, verbose, env, alpha, loadConstraint);
    }

    /**
     * Creates a new RefSfcPlacementSolver.
     *
     * @param verbose whether verbose output is wanted or not
     * @param env     the environment (passing this as a parameter requires only one license check )
     * @param alpha   the weight factor for the VNF deployment cost
     */
    public RefSfcPlacementSolver(boolean verbose, GRBEnv env, double alpha, Logger log) {
        super(OptimizationGoal.SPT, verbose, env, alpha, log);
    }

    @Override
    protected void addTreeVariables(GRBModel model) throws GRBException {
        // intentionally blank
    }

    @Override
    protected void addMTZConstraints(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        // intentionally blank
    }

    @Override
    protected void addInDegreeLessEqualOneConstraint(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        // intentionally blank
    }

    @Override
    protected NfvPlacementSolution.SolutionType getType() {
        return NfvPlacementSolution.SolutionType.SPT;
    }
}
