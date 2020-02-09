package org.onosproject.nfv.placement.solver.ilp32;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBQuadExpr;
import gurobi.GRBVar;
import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.nfv.placement.solver.INfvPlacementSolver;
import org.onosproject.nfv.placement.solver.NfvPlacementRequest;
import org.onosproject.nfv.placement.solver.NfvPlacementSolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ILinkWeigher;
import thesiscode.common.topo.WrappedPoPVertex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Ilp32Solver implements INfvPlacementSolver {
    private Logger log = LoggerFactory.getLogger(getClass());

    private Set<TopologyVertex> nodes;
    private Set<TopologyEdge> edges;
    private ILinkWeigher linkWeigher;
    private NprTraffic traffic;
    private NprNfvTypes.Type nfvType;


    private int N;
    private int K; // unlike in ref 32, K is here the amount of unidirectional edges
    private int n;

    private double[] p1;
    private double[] p2;
    private double[] p3;

    private int[][] m;
    private int[][] a;
    private int[] w;
    private int[] r;
    private int[][] q;
    private int[][] s;

    /*
        variables
         */
    private GRBVar[][] x1;
    private GRBVar[][] x2;
    private GRBVar[][] x;
    private GRBVar[][] mx1;
    private GRBVar[][] mx2;
    private GRBVar[] y1;
    private GRBVar[] y2;
    private GRBVar[] z;

    private double runtime;


    @Override
    public NfvPlacementSolution solve(NfvPlacementRequest req) {
        init(req);
        log.debug("finished init");

        NfvPlacementSolution sol = null;
        try {
            GRBEnv env = new GRBEnv("nfv.log");
            GRBModel model = new GRBModel(env);

            log.debug("finished creating env and model");

            addVariables(model);

            log.debug("finished adding variables");

            addObjective(model);

            log.debug("finished adding objective");

            addConstraints(model);

            log.debug("finished adding constraints");


            double beforeOptimize = System.currentTimeMillis();
            model.optimize();
            this.runtime = System.currentTimeMillis() - beforeOptimize;

            log.debug("finished optimizing, now extracting solution");

            sol = extractSolution();

            log.debug("finished extracting solution");

            model.dispose();
            env.dispose();

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

    private void init(NfvPlacementRequest req) {
        this.edges = req.getEdges();
        this.nodes = req.getVertices();
        this.linkWeigher = req.getLinkWeigher();

        List<NprTraffic> trafficList = req.getTraffic();
        if (trafficList.size() != 1) {
            throw new IllegalArgumentException("Traffic list must have a length of 1 for this solver.");
        }
        this.traffic = trafficList.get(0);

        List<NprNfvTypes.Type> sfc = traffic.getSfc();
        if (sfc.size() != 1) {
            throw new IllegalArgumentException("SFC must have a length of 1 for this solver.");
        }
        this.nfvType = sfc.get(0);

        initDataStructures();
        fillDataStructures();
    }

    private void initDataStructures() {
        N = nodes.size();
        K = edges.size();
        n = traffic.getEgressNodes().size();

        p1 = new double[K];
        p2 = new double[K];
        p2 = p1; // assume same cost before and after NFV processing
        p3 = new double[N];

        m = new int[N][K];
        a = new int[N][n];
        w = new int[n];
        r = new int[N];
        q = new int[N][n];
        s = new int[N][n];

        x1 = new GRBVar[K][n];
        x2 = new GRBVar[K][n];
        x = new GRBVar[K][n];
        mx1 = new GRBVar[N][n];
        mx2 = new GRBVar[N][n];
        y1 = new GRBVar[K];
        y2 = new GRBVar[K];
        z = new GRBVar[N];
    }

    private void fillDataStructures() {
        // p1, p2
        int entryCnt = 0;
        for (TopologyEdge edge : edges) {
            p1[entryCnt] = linkWeigher.getBandwidth(edge);
            // p2 not assigned, because p1==p2
            entryCnt++;
        }

        // p3
        entryCnt = 0;
        for (TopologyVertex node : nodes) {
            if (node instanceof WrappedPoPVertex) {
                p3[entryCnt] = ((WrappedPoPVertex) node).getDeploymentCost(NprNfvTypes.Type.TRANSCODER);
            } else {
                p3[entryCnt] = 0;
            }
            entryCnt++;
        }

        // m
        int columnCnt = 0;
        int rowCnt = 0;
        int incidence;
        for (TopologyEdge edge : edges) {
            for (TopologyVertex node : nodes) {
                DeviceId nodeId = node.deviceId();
                if (edge.src().deviceId().equals(nodeId)) { // node is edge source -> 1
                    incidence = 1;
                } else if (edge.dst().deviceId().equals(nodeId)) { // node is edge destination -> -1
                    incidence = -1;
                } else { //
                    incidence = 0;
                }
                m[rowCnt][columnCnt] = incidence;
                rowCnt++;
            }
            columnCnt++;
            rowCnt = 0;
        }

        // a, s, q
        rowCnt = 0;
        columnCnt = 0;
        boolean isSource;
        for (TopologyVertex node : nodes) {
            DeviceId nodeId = node.deviceId();

            isSource = traffic.getIngressNode().deviceId().equals(nodeId);

            for (TopologyVertex egress : traffic.getEgressNodes()) {
                if (isSource) {
                    s[rowCnt][columnCnt] = 1;
                    a[rowCnt][columnCnt] = 1;
                } else {
                    s[rowCnt][columnCnt] = 0;
                    if (nodeId.equals(egress.deviceId())) {
                        a[rowCnt][columnCnt] = -1;
                    } else {
                        a[rowCnt][columnCnt] = 0;
                    }
                }

                if (nodeId.equals(egress.deviceId())) {
                    q[rowCnt][columnCnt] = 0;
                } else {
                    q[rowCnt][columnCnt] = -1;
                }

                columnCnt++;
            }
            rowCnt++;
            columnCnt = 0;
        }

        // w
        for (int i = 0; i < w.length; i++) {
            w[i] = 1;
        }

        // r
        entryCnt = 0;
        int isNfvCapable;
        for (TopologyVertex node : nodes) {
            if (node instanceof WrappedPoPVertex) {
                isNfvCapable = 0; // on purpose: 0 means NFV capable in the MILP formulation
            } else {
                isNfvCapable = 1;
            }

            r[entryCnt++] = isNfvCapable;
        }
    }

    private void addVariables(GRBModel model) throws GRBException {
        // X1, X2, X
        for (int i = 0; i < K; i++) {
            for (int j = 0; j < n; j++) {
                x1[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x1_" + i + "_" + j);
                x2[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x2_" + i + "_" + j);
                x[i][j] = model.addVar(0.0, 2.0, 0.0, GRB.INTEGER, "x_" + i + "_" + j);
            }
        }

        // MX1, MX2
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < n; j++) {
                mx1[i][j] = model.addVar(-K, K, 0.0, GRB.INTEGER, "mx1_" + i + "_" + j);
                mx2[i][j] = model.addVar(-K, K, 0.0, GRB.INTEGER, "mx2_" + i + "_" + j);
            }
        }

        // Y1, Y2
        for (int i = 0; i < K; i++) {
            y1[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y1_" + i);
            y2[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y2_" + i);
        }

        // Z
        for (int i = 0; i < N; i++) {
            z[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "z_" + i);
        }
    }

    private void addObjective(GRBModel model) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        // objective: minimize P1^T*Y1 + P2^T*Y2 + P3^T*Z
        // P1^T * Y1
        for (int i = 0; i < K; i++) {
            expr.addTerm(p1[i], y1[i]);
        }

        // P2^T * Y2
        for (int i = 0; i < K; i++) {
            expr.addTerm(p2[i], y2[i]);
        }

        // P3^T * Z
        for (int i = 0; i < N; i++) {
            expr.addTerm(p3[i], z[i]);
        }

        model.setObjective(expr, GRB.MINIMIZE);
    }

    private void addConstraints(GRBModel model) throws GRBException {
        // X = X1 + X2
        for (int i = 0; i < K; i++) {
            for (int j = 0; j < n; j++) {
                GRBLinExpr expr = new GRBLinExpr();
                expr.addTerm(1.0, x1[i][j]);
                expr.addTerm(1.0, x2[i][j]);
                model.addConstr(expr, GRB.EQUAL, x[i][j], "constr_X=X1+X2_" + i + "_" + j);
            }
        }


        // M * (X1+X2) = M*X = A
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < n; j++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int u = 0; u < K; u++) {
                    expr.addTerm(m[i][u], x[u][j]);
                }
                model.addConstr(expr, GRB.EQUAL, a[i][j], "constr_M*X=A_" + i + "_" + j);

            }
        }

        ////////////
        // MX1 = M * X1
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < n; j++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int u = 0; u < K; u++) {
                    expr.addTerm(m[i][u], x1[u][j]);
                }
                model.addConstr(expr, GRB.EQUAL, mx1[i][j], "constr_MX1=M*X1_" + i + "_" + j);
            }
        }

        // Z^T * M * X1 = -W
        for (int i = 0; i < n; i++) {
            GRBQuadExpr expr = new GRBQuadExpr();
            for (int j = 0; j < N; j++) {
                expr.addTerm(1.0, mx1[j][i], z[j]);
            }
            model.addQConstr(expr, GRB.EQUAL, -w[i], "constr_Z^T*M*X1=-W_" + i);
        }
        ////////////

        ////////////
        // MX2 = M * X2
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < n; j++) {
                GRBLinExpr expr = new GRBLinExpr();
                for (int u = 0; u < K; u++) {
                    expr.addTerm(m[i][u], x2[u][j]);
                }
                model.addConstr(expr, GRB.EQUAL, mx2[i][j], "constr_MX2=M*X2_" + i + "_" + j);
            }
        }

        // Z^T * M * X2 = W
        for (int i = 0; i < n; i++) {
            GRBQuadExpr expr = new GRBQuadExpr();
            for (int j = 0; j < N; j++) {
                expr.addTerm(1.0, mx2[j][i], z[j]);
            }
            model.addQConstr(expr, GRB.EQUAL, w[i], "constr_Z^T*M*X2=W_" + i);
        }
        ////////////

        // X1 <= Y1 <= 1
        for (int i = 0; i < K; i++) {
            model.addConstr(y1[i], GRB.LESS_EQUAL, 1.0, "constr_Y1<=1_" + i);
            for (int j = 0; j < n; j++) {
                model.addConstr(x1[i][j], GRB.LESS_EQUAL, y1[i], "constr_X1<=Y1_" + i + "_" + j);
            }
        }

        // X2 <= Y2 <= 1
        for (int i = 0; i < K; i++) {
            model.addConstr(y2[i], GRB.LESS_EQUAL, 1.0, "constr_Y2<=1_" + i);
            for (int j = 0; j < n; j++) {
                model.addConstr(x2[i][j], GRB.LESS_EQUAL, y2[i], "constr_X2<=Y2_" + i + "_" + j);
            }
        }

        // R^T * Z = 0
        GRBLinExpr expr = new GRBLinExpr();
        for (int i = 0; i < N; i++) {
            expr.addTerm(r[i], z[i]);
        }
        model.addConstr(expr, GRB.EQUAL, 0.0, "constr_R^T*Z=0");

        // Q <= M * X1 <= S
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < n; j++) {
                model.addConstr(q[i][j], GRB.LESS_EQUAL, mx1[i][j], "constr_Q<=MX1_" + i + "_" + j);
                model.addConstr(mx1[i][j], GRB.LESS_EQUAL, s[i][j], "constr_MX1<=S_" + i + "_" + j);
            }
        }


    }

    private NfvPlacementSolution extractSolution() throws GRBException {
        /*System.out.println("MX1");
        for (GRBVar[] u : mx1) {
            for (GRBVar v : u) {
                System.out.print((int) v.get(GRB.DoubleAttr.X) + "|");
            }
            System.out.println();
        }


        System.out.println("****X1:");
        for (GRBVar[] u : x1) {
            for (GRBVar v : u) {
                System.out.print((int) v.get(GRB.DoubleAttr.X) + "|");
            }
            System.out.println();
        }

        System.out.println("****X2:");
        for (GRBVar[] u : x2) {
            for (GRBVar v : u) {
                System.out.print((int) v.get(GRB.DoubleAttr.X) + "|");
            }
            System.out.println();
        }

        System.out.println("****X:");
        for (GRBVar[] u : x) {
            for (GRBVar v : u) {
                System.out.print((int) v.get(GRB.DoubleAttr.X) + "|");
            }
            System.out.println();
        }*/

        Set<TopologyEdge> solutionEdges = new HashSet<>();

        // System.out.println("**links used to connect source to VNF**");
        TopologyEdge[] edgeArr = edges.toArray(new TopologyEdge[0]);
        int cnt = 0;
        for (GRBVar u : y1) {
            if (u.get(GRB.DoubleAttr.X) == 1.0) {
                solutionEdges.add(edgeArr[cnt]);
                //System.out.println(String.format("%s -> %s", edgeArr[cnt].src().deviceId(), edgeArr[cnt].dst()
                // .deviceId()));
            }
            cnt++;
        }

        //System.out.println("**links used to connect VNF to destinations**");
        cnt = 0;
        for (GRBVar u : y2) {
            if (u.get(GRB.DoubleAttr.X) == 1.0) {
                solutionEdges.add(edgeArr[cnt]);
                /*System.out.println(String.format("%s -> %s", edgeArr[cnt].src().deviceId(), edgeArr[cnt]
                        .dst()
                        .deviceId()));*/
            }
            cnt++;
        }

        Map<NprTraffic, Set<TopologyEdge>> solutionEdgesMap = new HashMap<>();
        solutionEdgesMap.put(traffic, solutionEdges);

        Map<NprNfvTypes.Type, Set<TopologyVertex>> placement = new HashMap<>();

        //System.out.println("**VNF Placement**");
        cnt = 0;
        TopologyVertex[] nodesArr = nodes.toArray(new TopologyVertex[0]);
        for (GRBVar v : z) {
            if (v.get(GRB.DoubleAttr.X) == 1.0) {
                Set<TopologyVertex> oldSet = placement.get(nfvType);
                TopologyVertex currentNode = nodesArr[cnt];
                if (oldSet == null) {
                    oldSet = new HashSet<>();
                    oldSet.add(currentNode);
                    placement.put(nfvType, oldSet);
                } else {
                    oldSet.add(currentNode);
                }
                //System.out.println("place VNF @" + nodesArr[cnt].deviceId());
            }
            cnt++;
        }
        log.info("creating nfv placement solution, solution edges{}, placement{}", solutionEdges, placement);
        //return new NfvPlacementSolution(solutionEdgesMap, placement);
        return null;
    }

    // mainly used for testing

    int getN() {
        return N;
    }

    int getn() {
        return n;
    }

    double[] getP1() {
        return p1;
    }

    public double[] getP2() {
        return p2;
    }

    double[] getP3() {
        return p3;
    }

    int[][] getM() {
        return m;
    }

    int[][] getA() {
        return a;
    }

    int[] getW() {
        return w;
    }

    int[] getR() {
        return r;
    }

    int[][] getQ() {
        return q;
    }

    int[][] getS() {
        return s;
    }

    int getK() {
        return K;
    }
}
