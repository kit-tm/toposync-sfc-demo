package thesiscode.common.nfv.placement.solver.ilp32;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBQuadExpr;
import gurobi.GRBVar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardCodedIlp32TopoRef20 {
    private Logger log = LoggerFactory.getLogger(getClass());

    /*
    input
     */
    private final static int LINK_COST = 1;
    private final static int NODE_COST = 10;

    private final static int N = 8;
    private final static int K = 12;
    private final static int n = 3;

    private static double[] p1 = new double[2 * K];
    private static double[] p2 = p1;
    private static double[] p3 = new double[N];

    private static int[][] m = new int[N][2 * K];
    private static int[][] a = new int[N][n];
    private static int[] w = new int[n];
    private static int[] r = new int[N];
    private static int[][] q = new int[N][n];
    private static int[][] s = new int[N][n];

    /*
    variables
     */
    private static GRBVar[][] x1 = new GRBVar[2 * K][n];
    private static GRBVar[][] x2 = new GRBVar[2 * K][n];
    private static GRBVar[][] x = new GRBVar[2 * K][n];
    private static GRBVar[][] mx1 = new GRBVar[N][n];
    private static GRBVar[][] mx2 = new GRBVar[N][n];
    private static GRBVar[] y1 = new GRBVar[2 * K];
    private static GRBVar[] y2 = new GRBVar[2 * K];
    private static GRBVar[] z = new GRBVar[N];


    public static void main(String[] args) {
        init();

        try {
            GRBEnv env = new GRBEnv("nfv.log");
            GRBModel model = new GRBModel(env);

            addVariables(model);

            addObjective(model);

            addConstraints(model);

            model.optimize();

            printSolution();

            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        for (int i = 0; i < 2 * K; i++) {
            p1[i] = LINK_COST;
            p2[i] = LINK_COST;
        }

        for (int i = 0; i < N; i++) {
            p3[i] = NODE_COST;
        }

        m[0][0] = 1;
        m[1][0] = -1;

        m[1][1] = 1;
        m[0][1] = -1;

        m[0][2] = 1;
        m[2][2] = -1;

        m[2][3] = 1;
        m[0][3] = -1;

        m[1][4] = 1;
        m[3][4] = -1;

        m[3][5] = 1;
        m[1][5] = -1;

        m[3][6] = 1;
        m[5][6] = -1;

        m[5][7] = 1;
        m[3][7] = -1;

        m[5][8] = 1;
        m[4][8] = -1;

        m[4][9] = 1;
        m[5][9] = -1;

        m[4][10] = 1;
        m[7][10] = -1;

        m[7][11] = 1;
        m[4][11] = -1;

        m[7][12] = 1;
        m[6][12] = -1;

        m[6][13] = 1;
        m[7][13] = -1;

        m[6][14] = 1;
        m[2][14] = -1;

        m[2][15] = 1;
        m[6][15] = -1;

        m[2][16] = 1;
        m[3][16] = -1;

        m[3][17] = 1;
        m[2][17] = -1;

        m[4][18] = 1;
        m[3][18] = -1;

        m[3][19] = 1;
        m[4][19] = -1;

        m[4][20] = 1;
        m[2][20] = -1;

        m[2][21] = 1;
        m[4][21] = -1;

        m[4][22] = 1;
        m[6][22] = -1;

        m[6][23] = 1;
        m[4][23] = -1;

        a[0][0] = -1;
        a[5][1] = -1;
        a[6][0] = 1;
        a[6][1] = 1;
        a[6][2] = 1;
        a[7][2] = -1;

        for (int i = 0; i < w.length; i++) {
            w[i] = 1;
        }

        for (int i = 0; i < r.length; i++) {
            r[i] = 0;
        }

        for (int i = 0; i < q.length; i++) {
            for (int j = 0; j < q[0].length; j++) {
                if ((i == 0 && j == 0) || (i == 5 && j == 1) || (i == 7 && j == 2)) {
                    q[i][j] = 0;
                } else {
                    q[i][j] = -1;
                }
            }
        }

        s[6][0] = 1;
        s[6][1] = 1;
        s[6][2] = 1;
    }

    private static void addVariables(GRBModel model) throws GRBException {
        // X1, X2, X
        for (int i = 0; i < 2 * K; i++) {
            for (int j = 0; j < n; j++) {
                x1[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x1_" + i + "_" + j);
                x2[i][j] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x2_" + i + "_" + j);
                x[i][j] = model.addVar(0.0, 2.0, 0.0, GRB.INTEGER, "x_" + i + "_" + j);
            }
        }

        // MX1, MX2
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < n; j++) {
                mx1[i][j] = model.addVar(-2 * K, 2 * K, 0.0, GRB.INTEGER, "mx1_" + i + "_" + j);
                mx2[i][j] = model.addVar(-2 * K, 2 * K, 0.0, GRB.INTEGER, "mx2_" + i + "_" + j);
            }
        }

        // Y1, Y2
        for (int i = 0; i < 2 * K; i++) {
            y1[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y1_" + i);
            y2[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y2_" + i);
        }

        // Z
        for (int i = 0; i < N; i++) {
            z[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "z_" + i);
        }
    }

    private static void addObjective(GRBModel model) throws GRBException {
        GRBLinExpr expr = new GRBLinExpr();
        // objective: minimize P1^T*Y1 + P2^T*Y2 + P3^T*Z
        // P1^T * Y1
        for (int i = 0; i < 2 * K; i++) {
            expr.addTerm(p1[i], y1[i]);
        }

        // P2^T * Y2
        for (int i = 0; i < 2 * K; i++) {
            expr.addTerm(p2[i], y2[i]);
        }

        // P3^T * Z
        for (int i = 0; i < N; i++) {
            expr.addTerm(p3[i], z[i]);
        }

        model.setObjective(expr, GRB.MINIMIZE);
    }

    private static void addConstraints(GRBModel model) throws GRBException {
        // TODO X1 + X2 extra?
        // X = X1 + X2
        for (int i = 0; i < 2 * K; i++) {
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
                for (int u = 0; u < 2 * K; u++) {
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
                for (int u = 0; u < 2 * K; u++) {
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
                for (int u = 0; u < 2 * K; u++) {
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
        for (int i = 0; i < 2 * K; i++) {
            model.addConstr(y1[i], GRB.LESS_EQUAL, 1.0, "constr_Y1<=1_" + i);
            for (int j = 0; j < n; j++) {
                model.addConstr(x1[i][j], GRB.LESS_EQUAL, y1[i], "constr_X1<=Y1_" + i + "_" + j);
            }
        }

        // X2 <= Y2 <= 1
        for (int i = 0; i < 2 * K; i++) {
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

    public static int getn() {
        return n;
    }

    public static int getN() {
        return N;
    }

    public static double[] getP1() {
        return p1;
    }

    public static double[] getP2() {
        return p2;
    }

    public static double[] getP3() {
        return p3;
    }

    public static int[][] getM() {
        return m;
    }

    public static int[][] getA() {
        return a;
    }

    public static int[] getW() {
        return w;
    }

    public static int[] getR() {
        return r;
    }

    public static int[][] getQ() {
        return q;
    }

    public static int[][] getS() {
        return s;
    }

    public static int getK() {
        return K;
    }

    private static void printSolution() throws GRBException {
        System.out.println("MX1");
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
        }

        System.out.println("****Z:");
        for (GRBVar v : z) {
            System.out.println((int) v.get(GRB.DoubleAttr.X) + "|");
        }


    }

}
