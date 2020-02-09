package org.onosproject.nfv.placement.solver;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for all ILP implementations which implements the solve method. Template methods are called to add
 * the variables, constraints and call the actual optimization process.
 */
public abstract class AbstractNfvIlpPlacementSolver implements INfvPlacementSolver {
    private final static String ILP_FILE_PATH = "/home/felix/gurobi.ilp";
    private Logger log = LoggerFactory.getLogger(getClass());
    protected GRBEnv env;
    protected boolean verbose; // flag to indicate whether verbose output is wanted
    protected double runtime = 0; // stores the last runtime (i.e. the runtime of model.optimize();)
    protected double modelTime = 0;

    @Override
    public NfvPlacementSolution solve(NfvPlacementRequest req) {
        NfvPlacementSolution sol = null;
        GRBModel model = null;
        try {
            // create a new model
            model = new GRBModel(env);
            log.debug("finished creating model");

            // let the solver initialize itself and add all variables to the model
            double beforeModeling = System.currentTimeMillis();
            init(req, model);
            addVariables(model);
            log.debug("finished init and adding variables");

            // add the objective(s) to the model
            addObjective(model);
            log.debug("finished adding objective");

            // add all constraints to the model
            print("start adding constraints");
            addConstraints(model);
            print("finished adding constraints");
            modelTime = System.currentTimeMillis() - beforeModeling;

            // adapt verbosity of Gurobi
            if (!verbose) {
                model.set(GRB.IntParam.OutputFlag, 0);
            }

            print("start optimizing");
            // this is the actual optimization. the runtime of it is stored
            double beforeOptimizing = System.currentTimeMillis();
            model.optimize();
            runtime = System.currentTimeMillis() - beforeOptimizing;
            print("finished optimizing");

            /*
             * if during optimization the model was found to be infeasible, an Irreducible Infeasible Set (IIS) is
             * computed which contains the minimum set of variables and constraints which lead to the infeasibility.
             * the IIS is then written to a file.
             * great tool for debugging the model.
             */
            if (model.get(GRB.IntAttr.Status) == GRB.Status.INFEASIBLE) {
                model.computeIIS();
                model.write(ILP_FILE_PATH);
                System.err.println("Model was infeasible, IIS written to " + ILP_FILE_PATH + ". Returning null");
                //System.err.println("Model was infeasible. Returning null");
                return null;
            }
            log.debug("successfully finished optimizing, now extracting solution");

            /*
             * extract the solution from the variables and create a better-understandable representation of the solution
             * by using the NfvPlacementSolution class.
             */
            sol = extractSolution(model);
            log.debug("finished extracting solution");

            // finished, throw the model away.
            model.dispose();
        } catch (GRBException e) {
            for (StackTraceElement ste : e.getStackTrace()) {
                log.error(ste.toString());
            }
            e.printStackTrace();
        }
        return sol;
    }

    @Override
    public double getLastRuntime() {
        return runtime;
    }

    public double getModelTime() {
        return modelTime;
    }

    /**
     * Convenience method to print a String if the verbose flag is set.
     *
     * @param toPrint the string to print if the verbose flag is set.
     */
    protected void print(String toPrint) {
        if (verbose) {
            System.out.println(toPrint);
        }
    }

    // template methods
    protected abstract void init(NfvPlacementRequest req, GRBModel model) throws GRBException;

    protected abstract void addVariables(GRBModel model) throws GRBException;

    protected abstract void addObjective(GRBModel model) throws GRBException;

    protected abstract void addConstraints(GRBModel model) throws GRBException;

    protected abstract NfvPlacementSolution extractSolution(GRBModel model) throws GRBException;
}
