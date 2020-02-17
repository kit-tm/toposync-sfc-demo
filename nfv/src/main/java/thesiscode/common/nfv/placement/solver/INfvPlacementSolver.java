package thesiscode.common.nfv.placement.solver;

public interface INfvPlacementSolver {

    NfvPlacementSolution solve(NfvPlacementRequest req);

    double getLastRuntime();

}
