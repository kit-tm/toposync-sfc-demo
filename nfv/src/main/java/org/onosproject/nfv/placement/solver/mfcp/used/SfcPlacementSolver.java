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
import org.slf4j.Logger;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprResources;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ILinkWeigher;
import thesiscode.common.topo.WrappedPoPVertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Superclass for the SFC-TPL and SFC-ST formulations. Implements common constraints and adds the tree constraints
 * per template methods which can then be implemented in the concrete TPL/ST classes.
 */
public abstract class SfcPlacementSolver extends AbstractNfvIlpPlacementSolver {
    private NfvPlacementRequest req;
    private ILinkWeigher linkWeigher;
    private List<NprTraffic> trafficNoSfc;

    Set<TopologyEdge> edges;
    Set<TopologyVertex> nodes;
    List<NprTraffic> trafficSfc;

    private OptimizationGoal goal;

    private Set<NprNfvTypes.Type> allTypes;


    /**
     * here, flows are represented by their index in traffic.
     * logical nodes are represented by indices in the nested list.
     */
    /*
     * logical indices: 0=src, 1 to |Psi|=VNFs of SFC
     * this is p_t^k(v).
     * flow -> logical node -> node -> {0,1}
     */ List<List<Map<TopologyVertex, GRBVar>>> pPlacedForFlow;

    /*
     * logical indices: 0=src,1 to |Psi|=VNFs of SFC,|Psi|+1 = dst
     * this is p_{t;d}^k(v).
     * flow -> logical node -> dst -> node -> {0,1}
     */
    private List<List<Map<TopologyVertex, Map<TopologyVertex, GRBVar>>>> pPlacedForFlowAndDest;

    /*
     * this is p^f(v).
     * VNF type -> node -> {0,1}
     */
    private Map<NprNfvTypes.Type, Map<TopologyVertex, GRBVar>> pTypePlaced;

    /**
     * here, flows are represented by their index in traffic.
     * logical edges are represented by indices in the nested list:
     * (0=src->Psi_1,...,|Psi|-1=last edge of sfc, |Psi| =last VNF to dest or pseudo dest)
     */
    /*
     * this is f_t^{(k,l)}(i,j).
     * flow -> logical edge -> edge -> {0,1}
     */ List<List<Map<TopologyEdge, GRBVar>>> fForLogical;

    /*
     * this is f_{t;d}^{(k,l)}(i,j).
     * flow -> dst-> logical edge -> physical edge -> {0,1}
     */
    private List<Map<TopologyVertex, List<Map<TopologyEdge, GRBVar>>>> fForLogicalAndDst;


    private Map<NprTraffic, Map<TopologyVertex, GRBVar>> delaySfc; // delay_t(d)
    private Map<NprTraffic, GRBVar> minDelayPerSfcFlow; // delay_t^{min}
    private Map<NprTraffic, GRBVar> maxDelayPerSfcFlow; // delay_t^{max}

    /**
     * for non-sfc flows
     */
    private NoSfcPlacementSolver noSfcPlacementSolver;


    private double alpha; // weight factor for the VNF deployment cost
    private int loadConstraint = Integer.MAX_VALUE;
    private Logger log;

    /**
     * Creates a new SfcPlacementSolver.
     *
     * @param goal           the goal to optimize
     * @param verbose        whether verbose output is wanted or not
     * @param env            the environment (passing this as a parameter requires only one license check )
     * @param alpha          the weight factor for the VNF deployment cost
     * @param loadConstraint the network load constraint value
     */
    public SfcPlacementSolver(OptimizationGoal goal, boolean verbose, GRBEnv env, double alpha, int loadConstraint) {
        this.goal = goal;
        this.alpha = alpha;
        this.loadConstraint = loadConstraint;
        super.verbose = verbose;
        super.env = env;
        this.noSfcPlacementSolver = new NoSfcPlacementSolver(goal, verbose, env);
    }

    /**
     * Creates a new SfcPlacementSolver.
     *
     * @param goal    the goal to optimize
     * @param verbose whether verbose output is wanted or not
     * @param env     the environment (passing this as a parameter requires only one license check )
     * @param alpha   the weight factor for the VNF deployment cost
     * @param log     logger
     */
    public SfcPlacementSolver(OptimizationGoal goal, boolean verbose, GRBEnv env, double alpha, Logger log) {
        this.goal = goal;
        this.alpha = alpha;
        this.log = log;
        super.verbose = verbose;
        super.env = env;
        this.noSfcPlacementSolver = new NoSfcPlacementSolver(goal, verbose, env);
    }

    @Override
    protected void init(NfvPlacementRequest req, GRBModel model) {
        this.req = req;
        edges = req.getEdges();
        nodes = req.getVertices();
        linkWeigher = req.getLinkWeigher();

        // subdivide the flow set into T_{SFC} and T_{noSFC}
        List<NprTraffic> traffic = req.getTraffic();
        trafficNoSfc = new ArrayList<>();
        trafficSfc = new ArrayList<>();
        for (NprTraffic flow : traffic) {
            if (flow.getSfc().isEmpty()) {
                trafficNoSfc.add(flow);
            } else {
                trafficSfc.add(flow);
            }
        }

        /*
         * create request for non-sfc flows and let the NoSfcPlacementSolver init itself
         */
        NfvPlacementRequest noSfcReq = new NfvPlacementRequest(nodes, edges, trafficNoSfc, linkWeigher);
        noSfcPlacementSolver.init(noSfcReq, model);

        // store all VNF types which are used by the flows
        allTypes = new HashSet<>();
        for (NprTraffic flow : traffic) {
            allTypes.addAll(flow.getSfc());
        }
    }

    @Override
    protected void addVariables(GRBModel model) throws GRBException {
        addNonSfcVariables(model);
        addSfcVariables(model);
    }

    /**
     * Add the variables for the non-sfc flows. Redirect the work to the noSfcPlacementSolver.
     *
     * @param model the model
     * @throws GRBException can be thrown by Gurobi
     */
    private void addNonSfcVariables(GRBModel model) throws GRBException {
        noSfcPlacementSolver.addVariables(model);
    }

    /**
     * Add the variables for the sfc flows.
     *
     * @param model the model
     * @throws GRBException can be thrown by Gurobi
     */
    private void addSfcVariables(GRBModel model) throws GRBException {
        // add the edge decision variables (the f variables) to the model
        addfForLogical(model);
        addfForLogicalAndDst(model);

        // add the placement decision variables (the p variables) to the model
        addpTypePlaced(model);
        addpPlacedForFlow(model);
        addpPlacedForFlowAndDest(model);

        // add the delay variables to the model
        addDelayVariables(model);

        /*
         * add the tree variables (template method!)
         * these are the MTZ variables in the ST and TPL case. in the ST case, the additional f variables have also to be added
         */
        addTreeVariables(model);
    }

    // template methods for the tree constraints
    protected abstract void addTreeVariables(GRBModel model) throws GRBException;

    protected abstract void addMTZConstraints(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException;

    protected abstract void addInDegreeLessEqualOneConstraint(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException;

    protected void addDelayVariables(GRBModel model) throws GRBException {
        delaySfc = new HashMap<>();
        minDelayPerSfcFlow = new HashMap<>();
        maxDelayPerSfcFlow = new HashMap<>();

        int flowCnt = 0;
        for (NprTraffic flow : trafficSfc) {
            Map<TopologyVertex, GRBVar> dstToVar = new HashMap<>();
            for (TopologyVertex dst : flow.getEgressNodes()) {
                String name = "delay_" + flowCnt + "_dst=" + dst.toString();
                dstToVar.put(dst, model.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.INTEGER, name));
            }
            delaySfc.put(flow, dstToVar);
            minDelayPerSfcFlow.put(flow, model.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.INTEGER,
                                                      "minDelaySfc_" + flowCnt));
            maxDelayPerSfcFlow.put(flow, model.addVar(0.0, Double.MAX_VALUE, 0.0, GRB.INTEGER,
                                                      "maxDelaySfc_" + flowCnt));
            flowCnt++;
        }
    }

    private void addfForLogical(GRBModel model) throws GRBException {
        fForLogical = new ArrayList<>();

        StringBuilder name;
        int flowCnt = 0;
        for (NprTraffic flow : trafficSfc) {
            List<Map<TopologyEdge, GRBVar>> logicList = new ArrayList<>();
            Map<TopologyEdge, GRBVar> edgeToUsed;
            for (int i = 0; i < flow.getSfc().size(); i++) {
                edgeToUsed = new HashMap<>();
                for (TopologyEdge edge : edges) {
                    name = new StringBuilder("f_" + flowCnt + "_log=");
                    if (i == 0) { // s -> Psi_1
                        name.append(flow.getIngressNode().toString()).append("->").append(flow.getSfc().get(0));
                    } else { // Psi_i-1 -> Psi_i
                        name.append(flow.getSfc().get(i - 1)).append("->").append(flow.getSfc().get(i));
                    }
                    name.append("_pyh=").append(edge.src().toString()).append("->").append(edge.dst().toString());
                    edgeToUsed.put(edge, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name.toString()));
                }
                logicList.add(edgeToUsed);
            }

            // Psi_|Psi| -> dst
            edgeToUsed = new HashMap<>();
            for (TopologyEdge edge : edges) {
                name = new StringBuilder("f_" + flowCnt + "_log=").append(flow.getSfc().get(flow.getSfc().size() - 1))
                                                                  .append("->")
                                                                  .append("pseudo-dest")
                                                                  .append("_phy=")
                                                                  .append(edge.src().toString())
                                                                  .append("->")
                                                                  .append(edge.dst().toString());
                edgeToUsed.put(edge, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name.toString()));
            }
            logicList.add(edgeToUsed);


            fForLogical.add(logicList);
            flowCnt++;
        }
    }

    private void addfForLogicalAndDst(GRBModel model) throws GRBException {
        fForLogicalAndDst = new ArrayList<>();

        StringBuilder name;
        int flowCnt = 0;
        for (NprTraffic flow : trafficSfc) {
            Map<TopologyVertex, List<Map<TopologyEdge, GRBVar>>> dstToListMap = new HashMap<>();
            for (TopologyVertex dst : flow.getEgressNodes()) {
                List<Map<TopologyEdge, GRBVar>> logicList = new ArrayList<>();

                // from source to last vnf
                for (int i = 0; i <= flow.getSfc().size(); i++) {
                    Map<TopologyEdge, GRBVar> edgeToVar = new HashMap<>();
                    for (TopologyEdge edge : edges) {
                        name = new StringBuilder("f_" + flowCnt + "_dst=" + dst.toString() + "_log=");
                        if (i == 0) { // src -> first VNF
                            name.append(flow.getIngressNode().toString()).append("->").append(flow.getSfc().get(0));
                        } else if (i == flow.getSfc().size()) { // last VNF -> dst
                            name.append(flow.getSfc().get(i - 1)).append("->").append(dst);
                        } else { // SFC
                            name.append(flow.getSfc().get(i - 1)).append("->").append(flow.getSfc().get(i));
                        }
                        name.append("_pyh=").append(edge.src().toString()).append("->").append(edge.dst().toString());
                        edgeToVar.put(edge, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name.toString()));
                    }
                    logicList.add(edgeToVar);
                }

                dstToListMap.put(dst, logicList);
            }
            fForLogicalAndDst.add(dstToListMap);
            flowCnt++;
        }


    }


    private void addpTypePlaced(GRBModel model) throws GRBException {
        pTypePlaced = new HashMap<>();
        for (NprNfvTypes.Type type : allTypes) {
            Map<TopologyVertex, GRBVar> placedAtVert = new HashMap<>();
            for (TopologyVertex vert : nodes) {
                placedAtVert.put(vert, model.addVar(0.0, 1.0, 0.0, GRB.BINARY,
                                                    "p_" + type.name() + "_" + vert.toString()));
            }
            pTypePlaced.put(type, placedAtVert);
        }
    }

    private void addpPlacedForFlow(GRBModel model) throws GRBException {
        pPlacedForFlow = new ArrayList<>();
        int flowCnt = 0;
        for (NprTraffic flow : trafficSfc) {
            List<Map<TopologyVertex, GRBVar>> logicalList = new ArrayList<>();

            // src placed at vert?
            Map<TopologyVertex, GRBVar> placedAtVert = new HashMap<>();
            for (TopologyVertex vert : nodes) {
                String name = "p_t_" + flowCnt + "_" + flow.getIngressNode().toString() + "@" + vert.toString() + "?";
                placedAtVert.put(vert, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name));
            }
            logicalList.add(placedAtVert);

            // VNF placed at vert?
            for (NprNfvTypes.Type type : flow.getSfc()) {
                placedAtVert = new HashMap<>();
                for (TopologyVertex vert : nodes) {
                    String name = "p_t_" + flowCnt + "_" + type.name() + "@" + vert.toString() + "?";
                    placedAtVert.put(vert, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name));
                }
                logicalList.add(placedAtVert);
            }

            flowCnt++;
            pPlacedForFlow.add(logicalList);
        }

    }

    private void addpPlacedForFlowAndDest(GRBModel model) throws GRBException {
        pPlacedForFlowAndDest = new ArrayList<>();
        int flowCnt = 0;
        for (NprTraffic flow : trafficSfc) {
            List<Map<TopologyVertex, Map<TopologyVertex, GRBVar>>> logicalList = new ArrayList<>();

            // src placed at vert for dst?
            Map<TopologyVertex, Map<TopologyVertex, GRBVar>> dstToMap = new HashMap<>();
            for (TopologyVertex dst : flow.getEgressNodes()) {
                Map<TopologyVertex, GRBVar> placedAtVert = new HashMap<>();
                for (TopologyVertex vert : nodes) {
                    if (log != null) {
                        log.info("adding p, vert devId: {}", vert.toString());
                    }
                    String name =
                            "p_t_d_" + flowCnt + "_forDst=" + dst.toString() + "_" + flow.getIngressNode().toString() +
                                    "@" + vert.toString() + "?";

                    placedAtVert.put(vert, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name));
                }
                dstToMap.put(dst, placedAtVert);
            }
            logicalList.add(dstToMap);

            // VNF placed at vert for dst?
            for (NprNfvTypes.Type type : flow.getSfc()) {
                dstToMap = new HashMap<>();
                for (TopologyVertex dst : flow.getEgressNodes()) {
                    Map<TopologyVertex, GRBVar> placedAtVert = new HashMap<>();
                    for (TopologyVertex vert : nodes) {
                        String name = "p_t_d_" + flowCnt + "_forDst=" + dst.toString() + "_" + type.name() + "@" +
                                vert.toString() + "?";
                        placedAtVert.put(vert, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name));
                    }
                    dstToMap.put(dst, placedAtVert);
                }
                logicalList.add(dstToMap);
            }

            // dst placed at vert for dst?
            dstToMap = new HashMap<>();
            for (TopologyVertex dst : flow.getEgressNodes()) {
                Map<TopologyVertex, GRBVar> placedAtVert = new HashMap<>();
                for (TopologyVertex vert : nodes) {
                    String name = "p_t_d_" + flowCnt + "_forDst=" + dst.toString() + "@" + vert.toString() + "?";
                    placedAtVert.put(vert, model.addVar(0.0, 1.0, 0.0, GRB.BINARY, name));
                }
                dstToMap.put(dst, placedAtVert);
            }
            logicalList.add(dstToMap);

            pPlacedForFlowAndDest.add(logicalList);
            flowCnt++;
        }

    }

    @Override
    protected void addObjective(GRBModel model) throws GRBException {
        // sum over all t in T: delay_t^{max}-delay_t^{min}
        GRBLinExpr deviations = new GRBLinExpr();
        for (NprTraffic sfcFlow : trafficSfc) {
            deviations.addTerm(1.0, maxDelayPerSfcFlow.get(sfcFlow));
            deviations.addTerm(-1.0, minDelayPerSfcFlow.get(sfcFlow));
        }
        for (NprTraffic noSfcFlow : trafficNoSfc) {
            deviations.addTerm(1.0, noSfcPlacementSolver.getMaxDelayForFlow(noSfcFlow));
            deviations.addTerm(-1.0, noSfcPlacementSolver.getMinDelayForFlow(noSfcFlow));
        }

        GRBLinExpr objExpr = new GRBLinExpr();
        switch (goal) {
            case LOAD_REDUCTION:
                /*
                 * edge cost
                 */
                // no sfc case
                for (NprTraffic flow : trafficNoSfc) {
                    for (TopologyEdge edge : edges) {
                        objExpr.addTerm(flow.getDemand(), noSfcPlacementSolver.getIsEdgeUsedAtAllForFlow(flow, edge));
                    }
                }
                // sfc case
                for (int i = 0; i < trafficSfc.size(); i++) {
                    NprTraffic flow = trafficSfc.get(i);
                    for (TopologyEdge edge : edges) {
                        for (int j = 0; j < fForLogical.get(i).size(); j++) {
                            if (j < flow.getSfc().size()) {
                                objExpr.addTerm(flow.getDemand(), fForLogical.get(i).get(j).get(edge));
                            }
                        }
                    }
                }

                /*
                 * VNF deployment cost
                 */
                for (TopologyVertex vert : nodes) {
                    if (vert instanceof WrappedPoPVertex) {
                        WrappedPoPVertex wrappedPoPVertex = (WrappedPoPVertex) vert;
                        for (NprNfvTypes.Type type : allTypes) {
                            objExpr.addTerm(
                                    alpha * wrappedPoPVertex.getDeploymentCost(type), pTypePlaced.get(type).get(vert));
                        }
                    }

                }
                model.setObjective(objExpr, GRB.MINIMIZE);
                break;
            case SPT:
                for (NprTraffic flow : trafficSfc) { // delay for SFC flows
                    for (TopologyVertex dst : flow.getEgressNodes()) {
                        objExpr.addTerm(1.0, delaySfc.get(flow).get(dst));
                    }
                }
                for (NprTraffic flow : trafficNoSfc) { // delay for non-SFC flows
                    for (TopologyVertex dst : flow.getEgressNodes()) {
                        objExpr.addTerm(1.0, noSfcPlacementSolver.getDelayForFlowAndDestination(flow, dst));
                    }
                }
                /*
                 * VNF deployment cost
                 */
                for (TopologyVertex vert : nodes) {
                    if (vert instanceof WrappedPoPVertex) {
                        WrappedPoPVertex wrappedPoPVertex = (WrappedPoPVertex) vert;
                        for (NprNfvTypes.Type type : allTypes) {
                            objExpr.addTerm(
                                    alpha * wrappedPoPVertex.getDeploymentCost(type), pTypePlaced.get(type).get(vert));
                        }
                    }

                }
                model.setObjective(objExpr, GRB.MINIMIZE);
                break;
            case MIN_MAX_DELAYSUM_THEN_DEVIATION:
                // add max delays per sfc flow
                for (NprTraffic flow : trafficSfc) {
                    objExpr.addTerm(1.0, maxDelayPerSfcFlow.get(flow));
                }
                // add max delays for non sfc flows
                for (NprTraffic flow : trafficNoSfc) {
                    objExpr.addTerm(1.0, noSfcPlacementSolver.getMaxDelayForFlow(flow));
                }
                // add deployment cost
                for (NprNfvTypes.Type type : allTypes) {
                    for (TopologyVertex vertex : nodes) {
                        if (vertex instanceof WrappedPoPVertex) {
                            WrappedPoPVertex wrappedPoPVertex = (WrappedPoPVertex) vertex;
                            objExpr.addTerm(alpha * wrappedPoPVertex.getDeploymentCost(type), pTypePlaced.get(type)
                                                                                                         .get(vertex));
                        }
                    }
                }
                // higher priority objective: minimize sum of delay_t^{max} + alpha * deployment_cost
                model.setObjectiveN(objExpr, 1, 1, 1, 0, 0, "maxDelayPerFlowSum");

                // lower priority objective: minimize sum of delay_t^{max}-delay_t^{min}
                model.setObjectiveN(deviations, 2, 0, 1, 0, 0, "deviationSum");

                break;
            default:
                throw new IllegalStateException("Optimization goal not yet implemented in this solver.");
        }
    }

    @Override
    protected void addConstraints(GRBModel model) throws GRBException {
        if (loadConstraint != Integer.MAX_VALUE) {
            addLoadConstraint(model);
        }


        /*
         * per-flow constraints
         */
        addNonSfcPerFlowConstraints(model);

        print("added non sfc per flow constr");

        addSfcPerFlowConstraints(model);

        print("added sfc per flow constr");

        /*
         * "general" (i.e., not per-flow) constraints
         */
        addLinkCapacityNotExceededConstraint(model);

        addPoPCapacityNotExceededConstraint(model);

        addVnfsOnlyPlacedAtPoPsConstraint(model);

        //  p <= sum of p_t
        for (NprNfvTypes.Type type : allTypes) {
            for (TopologyVertex vert : nodes) {
                GRBVar pVar = pTypePlaced.get(type).get(vert);

                GRBLinExpr sumOfPT = new GRBLinExpr();
                for (int i = 0; i < trafficSfc.size(); i++) {
                    NprTraffic flow = trafficSfc.get(i);
                    for (int j = 1; j <= flow.getSfc().size(); j++) {
                        if (!trafficSfc.get(i).getSfc().get(j - 1).equals(type)) {
                            continue;
                        }
                        sumOfPT.addTerm(1.0, pPlacedForFlow.get(i).get(j).get(vert));
                    }
                }

                String name = "constr_p<=sumPt_" + type.name() + "_" + vert.toString();
                model.addConstr(pVar, GRB.LESS_EQUAL, sumOfPT, name);
            }
        }
    }

    protected void addLoadConstraint(GRBModel model) throws GRBException {
        GRBLinExpr load = new GRBLinExpr();
        // no sfc
        for (NprTraffic flow : trafficNoSfc) {
            for (TopologyEdge edge : edges) {
                load.addTerm(flow.getDemand(), noSfcPlacementSolver.getIsEdgeUsedAtAllForFlow(flow, edge));
            }
        }
        // sfc
        for (int i = 0; i < trafficSfc.size(); i++) {
            NprTraffic flow = trafficSfc.get(i);
            for (TopologyEdge edge : edges) {
                for (int j = 0; j < fForLogical.get(i).size(); j++) {
                    load.addTerm(flow.getDemand(), fForLogical.get(i).get(j).get(edge));
                }
            }
        }
        model.addConstr(load, GRB.LESS_EQUAL, loadConstraint, "load_constraint");
    }


    private void addVnfsOnlyPlacedAtPoPsConstraint(GRBModel model) throws GRBException {
        // VNFs only placed at PoPs
        for (NprNfvTypes.Type type : allTypes) {
            for (TopologyVertex vert : nodes) {
                int isPoP;
                if (vert instanceof WrappedPoPVertex) {
                    isPoP = 1;
                } else {
                    isPoP = 0;
                }
                if (log != null) {
                    log.info("{} is pop: {}", vert.toString(), isPoP);
                }
                model.addConstr(pTypePlaced.get(type).get(vert), GRB.LESS_EQUAL, isPoP,
                                "constr_only-placed-at-pops_" + type.name() + "_" + vert.toString());
            }
        }
    }

    private void addPoPCapacityNotExceededConstraint(GRBModel model) throws GRBException {
        // PoP capacity not exceeded
        for (NprResources resource : NprResources.values()) {
            for (TopologyVertex vertex : nodes) {
                if (vertex instanceof WrappedPoPVertex) {
                    WrappedPoPVertex wrappedPoPVertex = (WrappedPoPVertex) vertex;
                    GRBLinExpr sum = new GRBLinExpr();
                    for (NprNfvTypes.Type type : allTypes) {
                        sum.addTerm(NprNfvTypes.getRequirements(type).get(resource), pTypePlaced.get(type).get(vertex));
                    }
                    model.addConstr(sum, GRB.LESS_EQUAL, wrappedPoPVertex.getResourceCapacity(resource),
                                    "constr_pop_cap_" + resource.name() + "_" + vertex.toString());
                }
            }
        }
    }

    private void addLinkCapacityNotExceededConstraint(GRBModel model) throws GRBException {
        // edge capacity not exceeded
        for (TopologyEdge edge : edges) {
            GRBLinExpr sum = new GRBLinExpr();
            for (int flowCnt = 0; flowCnt < trafficSfc.size(); flowCnt++) {
                NprTraffic sfcFlow = trafficSfc.get(flowCnt);
                for (int j = 0; j < fForLogical.get(flowCnt).size(); j++) {
                    sum.addTerm(sfcFlow.getDemand(), fForLogical.get(flowCnt).get(j).get(edge));
                }
            }
            for (NprTraffic noSfcFlow : trafficNoSfc) {
                sum.addTerm(noSfcFlow.getDemand(), noSfcPlacementSolver.getIsEdgeUsedAtAllForFlow(noSfcFlow, edge));
            }
            String name = "constr_edge-cap_" + edge.src().toString() + "->" + edge.dst().toString();
            model.addConstr(sum, GRB.LESS_EQUAL, linkWeigher.getBandwidth(edge), name);
        }
    }

    private void addSfcPerFlowConstraints(GRBModel model) throws GRBException {
        int flowIndex = 0;
        for (NprTraffic flow : trafficSfc) {

            addDecisionVariableConnectionConstraints(model, flow, flowIndex);

            addFlowConservationConstraint(model, flow, flowIndex);

            addPlacedExactlyOnceConstraint(model, flow, flowIndex);

            addSourcePlacedConstraint(model, flow, flowIndex);
            addDestinationPlacedConstraint(model, flow, flowIndex);

            addDelayConstraints(model, flow, flowIndex);

            // calls template methods which are implemented in concrete subclasses
            addInDegreeLessEqualOneConstraint(model, flow, flowIndex);
            addMTZConstraints(model, flow, flowIndex);

            flowIndex++;
        }
    }

    private void addDecisionVariableConnectionConstraints(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        // f_{t;d} <= f_t
        for (int j = 0; j <= flow.getSfc().size(); j++) {
            for (TopologyEdge edge : edges) {
                for (TopologyVertex dst : flow.getEgressNodes()) {
                    String name =
                            "constr_f_{" + flowIndex + ";" + dst.toString() + "}^" + j + "(" + edge.src().toString() +
                                    "->" + edge.dst().toString() + ")<=f_" + flowIndex + "^" + j + "(" +
                                    edge.src().toString() + "->" + edge.dst().toString();
                    model.addConstr(fForLogicalAndDst.get(flowIndex)
                                                     .get(dst)
                                                     .get(j)
                                                     .get(edge), GRB.LESS_EQUAL, fForLogical.get(flowIndex)
                                                                                            .get(j)
                                                                                            .get(edge), name);
                }
            }
        }

        // f_t <= sum of f_{t;d}
        for (int j = 0; j <= flow.getSfc().size(); j++) {
            for (TopologyEdge edge : edges) {
                GRBLinExpr sumOfL = new GRBLinExpr();
                for (TopologyVertex dst : flow.getEgressNodes()) {
                    sumOfL.addTerm(1.0, fForLogicalAndDst.get(flowIndex).get(dst).get(j).get(edge));
                }
                String name =
                        "constr_f_" + flowIndex + "^" + j + "(" + edge.src().toString() + "->" + edge.dst().toString() +
                                ")<=sumFTD";
                model.addConstr(fForLogical.get(flowIndex).get(j).get(edge), GRB.LESS_EQUAL, sumOfL, name);
            }
        }


        // p_t <= p
        for (NprNfvTypes.Type type : allTypes) {
            for (TopologyVertex vert : nodes) {
                GRBVar pVar = pTypePlaced.get(type).get(vert);
                for (int j = 1; j <= flow.getSfc().size(); j++) {
                    if (!flow.getSfc().get(j - 1).equals(type)) {
                        continue;
                    }
                    GRBVar pTVar = pPlacedForFlow.get(flowIndex).get(j).get(vert);
                    String name = "constr_p_" + flowIndex + "^" + j + "(" + vert.toString() + ")<=p^" + j + "(" +
                            vert.toString() + ")";
                    model.addConstr(pTVar, GRB.LESS_EQUAL, pVar, name);
                }
            }
        }

        // p_{t;d} <= p_t
        for (int j = 0; j <= flow.getSfc().size(); j++) {
            for (TopologyVertex vert : nodes) {
                GRBVar pTVar = pPlacedForFlow.get(flowIndex).get(j).get(vert);

                for (TopologyVertex dst : flow.getEgressNodes()) {
                    GRBVar pTDVar = pPlacedForFlowAndDest.get(flowIndex).get(j).get(dst).get(vert);
                    model.addConstr(pTDVar, GRB.LESS_EQUAL, pTVar, "");
                }

            }
        }

        // p_t<=sum of p_{t;d}
        for (int j = 0; j <= flow.getSfc().size(); j++) {
            for (TopologyVertex vert : nodes) {
                GRBVar pTVar = pPlacedForFlow.get(flowIndex).get(j).get(vert);

                GRBLinExpr sumOfPTD = new GRBLinExpr();

                for (TopologyVertex dst : flow.getEgressNodes()) {
                    sumOfPTD.addTerm(1.0, pPlacedForFlowAndDest.get(flowIndex).get(j).get(dst).get(vert));
                }

                String name = "constr_p_{" + flowIndex + "}^" + j + "(" + vert.toString() + ")" + "<=" + "sumPTD";
                model.addConstr(pTVar, GRB.LESS_EQUAL, sumOfPTD, name);
            }
        }


    }

    private void addFlowConservationConstraint(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        // conservation
        for (int j = 0; j <= flow.getSfc().size(); j++) { // logical edge j (connects logical nodes j and j+1)
            for (TopologyVertex dst : flow.getEgressNodes()) {
                for (TopologyVertex vert : nodes) {
                    GRBLinExpr lhs = new GRBLinExpr();
                    GRBLinExpr in = new GRBLinExpr();
                    GRBLinExpr out = new GRBLinExpr();

                    for (TopologyEdge edge : edges) {
                        if (edge.src().equals(vert)) {
                            out.addTerm(1.0, fForLogicalAndDst.get(flowIndex).get(dst).get(j).get(edge));
                        } else if (edge.dst().equals(vert)) {
                            in.addTerm(1.0, fForLogicalAndDst.get(flowIndex).get(dst).get(j).get(edge));
                        }
                    }

                    lhs.add(out);
                    lhs.multAdd(-1.0, in);

                    GRBLinExpr rhs = new GRBLinExpr();
                    rhs.addTerm(1.0, pPlacedForFlowAndDest.get(flowIndex).get(j).get(dst).get(vert));
                    rhs.addTerm(-1.0, pPlacedForFlowAndDest.get(flowIndex).get(j + 1).get(dst).get(vert));

                    String name =
                            "constr_cons_" + flowIndex + "vert=" + vert.toString() + ",dst=" + dst.toString() + "log=" +
                                    j;
                    model.addConstr(lhs, GRB.EQUAL, rhs, name);
                }
            }
        }
    }


    private void addPlacedExactlyOnceConstraint(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        for (int j = 0; j < pPlacedForFlowAndDest.get(flowIndex).size(); j++) {
            for (TopologyVertex dst : flow.getEgressNodes()) {
                GRBLinExpr sum = new GRBLinExpr();
                for (TopologyVertex vert : nodes) {
                    sum.addTerm(1.0, pPlacedForFlowAndDest.get(flowIndex).get(j).get(dst).get(vert));
                }
                String name = "constr_placed-once_" + flowIndex + "," + j + "for=" + dst.toString();
                model.addConstr(sum, GRB.EQUAL, 1.0, name);
            }
        }
    }

    private void addSourcePlacedConstraint(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        // source placed
        for (TopologyVertex dst : flow.getEgressNodes()) {
            TopologyVertex src = flow.getIngressNode();
            String name = "constr_src-placed_" + flowIndex + "forDst=" + dst.toString() + ",@" + src.toString();
            if (log != null) {
                log.debug("dst: " + dst.toString());
                log.debug("pPlaced(idx): " + pPlacedForFlowAndDest.get(flowIndex));
                log.debug("pPlaced(idx,0): " + pPlacedForFlowAndDest.get(flowIndex).get(0));
                log.debug("pPlaced(idx,0,dst): " + pPlacedForFlowAndDest.get(flowIndex).get(0).get(dst));
                log.debug("src: " + src.toString());
                log.debug("keySet: {}", pPlacedForFlowAndDest.get(flowIndex).get(0).get(dst).keySet());
                log.debug("src contained in keySet: {}", pPlacedForFlowAndDest.get(flowIndex)
                                                                              .get(0)
                                                                              .get(dst)
                                                                              .keySet()
                                                                              .contains(src));
                log.debug("src hash code: " + src.hashCode());
                log.debug("hash codes: {}", pPlacedForFlowAndDest.get(flowIndex)
                                                                 .get(0)
                                                                 .get(dst)
                                                                 .keySet()
                                                                 .stream()
                                                                 .map(Object::hashCode)
                                                                 .collect(Collectors.toSet()));
                log.debug("pPlaced(full,dst,src): " + pPlacedForFlowAndDest.get(flowIndex).get(0).get(dst).get(src));
            }
            model.addConstr(pPlacedForFlowAndDest.get(flowIndex).get(0).get(dst).get(src), GRB.EQUAL, 1.0, name);
        }
    }


    private void addDestinationPlacedConstraint(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        // destinations placed
        for (TopologyVertex dst : flow.getEgressNodes()) {
            model.addConstr(pPlacedForFlowAndDest.get(flowIndex)
                                                 .get(pPlacedForFlowAndDest.get(flowIndex).size() - 1)
                                                 .get(dst)
                                                 .get(dst), GRB.EQUAL, 1.0, "");

        }
    }

    private void addDelayConstraints(GRBModel model, NprTraffic flow, int flowIndex) throws GRBException {
        // delay
        for (TopologyVertex dst : flow.getEgressNodes()) {
            GRBLinExpr delaySum = new GRBLinExpr();

            // link delay
            for (TopologyEdge edge : edges) {
                for (int j = 0; j < fForLogicalAndDst.get(flowIndex).get(dst).size(); j++) {
                    delaySum.addTerm(linkWeigher.getDelay(edge), fForLogicalAndDst.get(flowIndex)
                                                                                  .get(dst)
                                                                                  .get(j)
                                                                                  .get(edge));
                }
            }

            // VNF delay
            int vnfCnt = 1;
            for (NprNfvTypes.Type type : flow.getSfc()) {
                for (TopologyVertex vert : nodes) {
                    if (vert instanceof WrappedPoPVertex) {
                        WrappedPoPVertex wrappedPoPVertex = (WrappedPoPVertex) vert;
                        delaySum.addTerm(wrappedPoPVertex.getDelay(type), pPlacedForFlowAndDest.get(flowIndex)
                                                                                               .get(vnfCnt)
                                                                                               .get(dst)
                                                                                               .get(vert));
                    }
                }
                vnfCnt++;
            }
            model.addConstr(delaySum, GRB.EQUAL, delaySfc.get(flow).get(dst),
                            "delay_" + flowIndex + "_dst=" + dst.toString());

            // the min max delay constrs only work with the objective of TPL and ST
            if (goal == OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION) {
                model.addConstr(maxDelayPerSfcFlow.get(flow), GRB.GREATER_EQUAL, delaySfc.get(flow).get(dst), "");
                model.addConstr(minDelayPerSfcFlow.get(flow), GRB.LESS_EQUAL, delaySfc.get(flow).get(dst), "");
            }
        }

        if (goal != OptimizationGoal.MIN_MAX_DELAYSUM_THEN_DEVIATION) {
            model.addGenConstrMax(maxDelayPerSfcFlow.get(flow), delaySfc.get(flow).values().toArray(new GRBVar[0]), 0.0,
                                  "constr_maxDelay_" + flowIndex);

            model.addGenConstrMin(minDelayPerSfcFlow.get(flow), delaySfc.get(flow)
                                                                        .values()
                                                                        .toArray(new GRBVar[0]), Double.MAX_VALUE,
                                  "constr_minDelay_" + flowIndex);
        }


    }

    private void addNonSfcPerFlowConstraints(GRBModel model) throws GRBException {
        noSfcPlacementSolver.addDecisionVariableConnectionConstraints(model);
        noSfcPlacementSolver.addSourceFlowConservation(model);
        noSfcPlacementSolver.addDestinationFlowConservation(model);
        noSfcPlacementSolver.addTransitFlowConservation(model);
        noSfcPlacementSolver.addDelayConstraints(model);
        if (goal != OptimizationGoal.SPT) {
            noSfcPlacementSolver.addInDegreeLessEqualOneConstraint(model);
            noSfcPlacementSolver.addMTZConstraints(model);
        }
    }

    @Override
    protected NfvPlacementSolution extractSolution(GRBModel model) throws GRBException {
        print("This is the solution:");

        Map<NprTraffic, Map<NprNfvTypes.Type, Set<TopologyVertex>>> placements = new HashMap<>();
        print("overall VNF placements:");
        for (TopologyVertex vert : nodes) {
            for (NprNfvTypes.Type type : allTypes) {
                if (pTypePlaced.get(type).get(vert).get(GRB.DoubleAttr.X) != 0) {
                    print("  " + type.name() + "@" + vert.toString());

                }
            }
        }

        print("edges + placement per commodity:");
        Map<NprTraffic, Set<TopologyEdge>> solutionEdges = new HashMap<>();


        Map<NprTraffic, Double> maxDelayPerFlow = new HashMap<>();
        Map<NprTraffic, Double> deviationPerFlow = new HashMap<>();
        int deviationSum = 0;
        int networkLoad = 0;
        int delaySum = 0;

        for (NprTraffic noSfcFlow : trafficNoSfc) {
            print(noSfcFlow.toString());
            print("max delay:" + noSfcPlacementSolver.getMaxDelayForFlow(noSfcFlow).get(GRB.DoubleAttr.X));
            print("min delay:" + noSfcPlacementSolver.getMinDelayForFlow(noSfcFlow).get(GRB.DoubleAttr.X));


            double maxDelayOfFlow = Math.round(noSfcPlacementSolver.getMaxDelayForFlow(noSfcFlow)
                                                                   .get(GRB.DoubleAttr.X));
            double minDelayOfFlow = Math.round(noSfcPlacementSolver.getMinDelayForFlow(noSfcFlow)
                                                                   .get(GRB.DoubleAttr.X));
            maxDelayPerFlow.put(noSfcFlow, maxDelayOfFlow);
            deviationPerFlow.put(noSfcFlow, maxDelayOfFlow - minDelayOfFlow);
            deviationSum += maxDelayOfFlow;
            deviationSum -= minDelayOfFlow;


            delaySum += maxDelayOfFlow;

            for (TopologyVertex dst : noSfcFlow.getEgressNodes()) {
                print("delay for " + dst.toString() + ":" +
                              noSfcPlacementSolver.getDelayForFlowAndDestination(noSfcFlow, dst).get(GRB.DoubleAttr.X));
            }
            Set<TopologyEdge> edgesForTraffic = new HashSet<>();
            for (TopologyEdge edge : edges) {
                GRBVar var = noSfcPlacementSolver.getIsEdgeUsedAtAllForFlow(noSfcFlow, edge);
                double varValue = Math.round(var.get(GRB.DoubleAttr.X));
                if (varValue != 0) {
                    print("    " + var.get(GRB.StringAttr.VarName) + "=" + varValue);
                    edgesForTraffic.add(edge);
                    networkLoad += noSfcFlow.getDemand() * varValue;
                }
            }
            solutionEdges.put(noSfcFlow, edgesForTraffic);
        }

        Map<NprTraffic, List<Set<TopologyEdge>>> logicalToRealEdgesForTraffics = new HashMap<>();

        int flowCnt = 0;
        for (NprTraffic sfcFlow : trafficSfc) {

            print(sfcFlow.toString());

            double maxDelayOfFlow = Math.round(maxDelayPerSfcFlow.get(sfcFlow).get(GRB.DoubleAttr.X));
            double minDelayOfFlow = Math.round(minDelayPerSfcFlow.get(sfcFlow).get(GRB.DoubleAttr.X));

            maxDelayPerFlow.put(sfcFlow, maxDelayOfFlow);
            deviationPerFlow.put(sfcFlow, maxDelayOfFlow - minDelayOfFlow);

            print("max delay:" + maxDelayOfFlow);
            print("min delay:" + minDelayOfFlow);

            deviationSum += maxDelayOfFlow;
            deviationSum -= minDelayOfFlow;

            delaySum += maxDelayOfFlow;

            for (TopologyVertex dst : sfcFlow.getEgressNodes()) {
                print("delay for " + dst.toString() + ":" + delaySfc.get(sfcFlow).get(dst).get(GRB.DoubleAttr.X));
            }
            placements.computeIfAbsent(sfcFlow, k -> new HashMap<>());
            solutionEdges.computeIfAbsent(sfcFlow, k -> new HashSet<>());
            // placements
            print("placements:");
            for (int j = 0; j < pPlacedForFlow.get(flowCnt).size(); j++) {
                for (TopologyVertex vert : nodes) {
                    if (Math.round(pPlacedForFlow.get(flowCnt).get(j).get(vert).get(GRB.DoubleAttr.X)) != 0) {
                        if (j > 0 && j <= sfcFlow.getSfc().size()) {
                            NprNfvTypes.Type type = sfcFlow.getSfc().get(j - 1);
                            placements.get(sfcFlow).computeIfAbsent(type, k -> new HashSet<>());
                            placements.get(sfcFlow).get(type).add(vert);

                            StringBuilder toPrint = new StringBuilder(
                                    "  " + type.name() + "@" + vert.toString() + ", for: {");
                            for (TopologyVertex dst : sfcFlow.getEgressNodes()) {
                                if (pPlacedForFlowAndDest.get(flowCnt)
                                                         .get(j)
                                                         .get(dst)
                                                         .get(vert)
                                                         .get(GRB.DoubleAttr.X) != 0) {
                                    toPrint.append(dst.toString()).append(",");
                                }
                            }
                            toPrint.append("}");
                            print(toPrint.toString());
                        }
                    }
                }
            }

            List<Set<TopologyEdge>> logicalToRealEdges = new ArrayList<>();

            // edges
            print("edges:");
            for (int jl = 0; jl < fForLogical.get(flowCnt).size(); jl++) {
                Set<TopologyEdge> edgesPerLogical = new HashSet<>();


                if (jl == 0) {
                    print(" " + sfcFlow.getIngressNode().toString() + "->" + sfcFlow.getSfc().get(0) + ":");
                    for (TopologyEdge edge : edges) {
                        if (Math.round(fForLogical.get(flowCnt).get(jl).get(edge).get(GRB.DoubleAttr.X)) != 0) {
                            edgesPerLogical.add(edge);
                            solutionEdges.get(sfcFlow).add(edge);
                            networkLoad +=
                                    Math.round(fForLogical.get(flowCnt).get(jl).get(edge).get(GRB.DoubleAttr.X)) *
                                            sfcFlow.getDemand();
                            print("  " + edge.src().toString() + "->" + edge.dst().toString());
                        }
                    }
                } else if (jl < sfcFlow.getSfc().size()) {
                    print(" " + sfcFlow.getSfc().get(jl - 1).name() + "->" + sfcFlow.getSfc().get(jl).name() + ":");
                    for (TopologyEdge edge : edges) {
                        if (Math.round(fForLogical.get(flowCnt).get(jl).get(edge).get(GRB.DoubleAttr.X)) != 0) {
                            edgesPerLogical.add(edge);
                            solutionEdges.get(sfcFlow).add(edge);
                            networkLoad +=
                                    Math.round(fForLogical.get(flowCnt).get(jl).get(edge).get(GRB.DoubleAttr.X)) *
                                            sfcFlow.getDemand();
                            print("  " + edge.src().toString() + "->" + edge.dst().toString());
                        }
                    }
                } else {
                    print(" " + sfcFlow.getSfc().get(sfcFlow.getSfc().size() - 1).name() + "-> pseudo-dest:");
                    for (TopologyEdge edge : edges) {
                        if (Math.round(fForLogical.get(flowCnt).get(jl).get(edge).get(GRB.DoubleAttr.X)) != 0) {
                            edgesPerLogical.add(edge);
                            solutionEdges.get(sfcFlow).add(edge);
                            networkLoad +=
                                    Math.round(fForLogical.get(flowCnt).get(jl).get(edge).get(GRB.DoubleAttr.X)) *
                                            sfcFlow.getDemand();
                            print("  " + edge.src().toString() + "->" + edge.dst().toString());
                        }
                    }
                }
                logicalToRealEdges.add(edgesPerLogical);
            }
            logicalToRealEdgesForTraffics.put(sfcFlow, logicalToRealEdges);
            flowCnt++;
        }

        NfvPlacementSolution sol = new NfvPlacementSolution(solutionEdges, placements, req, goal, model.get(GRB.DoubleAttr.ObjVal), deviationSum, delaySum, networkLoad, deviationPerFlow, maxDelayPerFlow);
        for (NprTraffic flow : logicalToRealEdgesForTraffics.keySet()) {
            sol.setLogicalEdgesForTraffic(flow, logicalToRealEdgesForTraffics.get(flow));
        }

        return sol;
    }

    @Override
    protected void print(String toPrint) {
        if (verbose) {
            if (log != null) {
                log.info(toPrint);
            } else {
                System.out.println(toPrint);
            }
        }
    }
}
