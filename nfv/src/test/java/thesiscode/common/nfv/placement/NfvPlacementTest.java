package thesiscode.common.nfv.placement;

import org.junit.Assert;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.nfv.placement.solver.INfvPlacementSolver;
import thesiscode.common.nfv.placement.solver.NfvPlacementRequest;
import thesiscode.common.nfv.placement.solver.NfvPlacementSolution;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ConstantLinkWeigher;
import thesiscode.common.topo.ILinkWeigher;
import thesiscode.common.util.RandomDemandGenerator;
import thesiscode.common.util.graph.GraphUtil;
import util.PlotStarter;
import util.mock.EvalTopoMockBuilder;
import util.mock.TestGraph;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class NfvPlacementTest {

    public static final String CSV_BASE_PATH = "/home/felix/Desktop/ba/code/eval/";

    /**
     * Tests a solver one time with the eval topo and a random demand generator.
     *
     * @param solver the solver to use for solving
     * @param plot   whether to plot the solution or not
     * @return the solution
     */
    protected NfvPlacementSolution solveRandomRequest(INfvPlacementSolver solver, boolean plot) {
        TestGraph graph = EvalTopoMockBuilder.getMockedTopo();

        ILinkWeigher lw = new ConstantLinkWeigher(10, 1);

        RandomDemandGenerator dg = new RandomDemandGenerator(graph, lw);
        List<NprTraffic> demand = dg.generateDemand();

        NfvPlacementRequest req = new NfvPlacementRequest(graph.getVertexes(), graph.getEdges(), demand, lw);

        NfvPlacementSolution sol = solver.solve(req);

        if (plot) {
            writeToCsvAndPlot(sol, CSV_BASE_PATH + System.currentTimeMillis());
        }

        return sol;
    }

    /**
     * Convenienve method to write the solution to a CSV by calling {@link NfvPlacementSolution#writeToCsv(String)}
     * and starting the plot by calling {@link PlotStarter#startPythonPlot(String)}.
     *
     * @param path the path to write csv and plot
     * @param sol  the solution to write to csv and plot
     */
    protected void writeToCsvAndPlot(NfvPlacementSolution sol, String path) {
        try {
            sol.writeToCsv(path);
            PlotStarter.startPythonPlot(path);
        } catch (FileNotFoundException e) {
            Assert.fail("File not found while writing solution to csv. " + e.getMessage());
        } catch (IOException e) {
            Assert.fail("IOException during python script execution" + e.getMessage());
        }
    }

    public void plotIfNotExpected(NfvPlacementSolution sol, Function<NfvPlacementSolution, Boolean> func, boolean expected, String path) {
        boolean actual = func.apply(sol);
        if (actual != expected) {
            writeToCsvAndPlot(sol, path);
            Assert.assertEquals(expected, actual);
        }
    }


    /*protected boolean sfcCorrect(NprTraffic traffic, Set<TopologyEdge> edges, Map<NprNfvTypes.Type,
            Set<TopologyVertex>> placements) {
        Map<TopologyVertex, Set<NprNfvTypes.Type>> placementsPerNode = new HashMap<>();
        placements.forEach((type, set) -> {
            for (TopologyVertex node : set) {
                placementsPerNode.computeIfAbsent(node, n -> new HashSet<>());
                placementsPerNode.get(node).add(type);
            }
        });
        System.out.println(placementsPerNode);


        List<NprNfvTypes.Type> sfc = traffic.getSfc();

        // actual sfc as it occured in the network, contains the types grouped by node
        List<Set<NprNfvTypes.Type>> actualSfc = new ArrayList<>();

        actualSfc.add(placementsPerNode.get(traffic.getIngressNode()));

        Map<TopologyVertex, List<TopologyEdge>> edgesBySrc = getEdgesBySource(edges);

        boolean sfcCorrect = true;

        for (TopologyEdge edge : edgesBySrc.get(traffic.getIngressNode())) {
            sfcCorrect = dfsSfc(edge.dst(), edgesBySrc, sfc, actualSfc, placementsPerNode);
        }

        return sfcCorrect;
    }*/

    /*private boolean dfsSfc(TopologyVertex current, Map<TopologyVertex, List<TopologyEdge>> edgesBySrc,
                           List<NprNfvTypes.Type> sfc, List<Set<NprNfvTypes.Type>> actualSfc, Map<TopologyVertex,
            Set<NprNfvTypes.Type>> placementsPerNode, Map<TopologyVertex, Boolean> visited) {
        Set<NprNfvTypes.Type> vnfsAtCurrent = placementsPerNode.get(current);
        if (vnfsAtCurrent != null && vnfsAtCurrent.size() > 0) {
            actualSfc.add(vnfsAtCurrent);
        }


        List<TopologyEdge> edgesFromCurrent = edgesBySrc.get(current);
        if (edgesFromCurrent == null) { // destination reached, check if whole SFC correct
            int start = 0;
            for (int j = 0; j < actualSfc.size(); j++) {
                Set<NprNfvTypes.Type> actualTypes = actualSfc.get(j);
                for (int i = start; i < start + actualTypes.size(); i++) {
                    System.out.println("checking if " + sfc.get(i).name() + " is in " + actualTypes.toString());
                    if (actualTypes.contains(sfc.get(i))) {
                        if (j == actualSfc.size() - 1) {
                            start += actualTypes.size();
                        }
                        continue;
                    } else {
                        return false;
                    }
                }
            }
        } else {
            for (TopologyEdge edge : edgesFromCurrent) {
                TopologyVertex edgeDst = edge.dst();
                return dfsSfc(edgeDst, edgesBySrc, sfc, actualSfc, placementsPerNode);
            }
        }

        return true;
    }*/

    public boolean allEdgesReachableFromSource(NfvPlacementSolution sol) {
        for (NprTraffic flow : sol.getRequest().getTraffic()) {
            Set<TopologyEdge> solutionEdges = sol.getSolutionEdgesByTraffic(flow);

            Map<TopologyEdge, Boolean> edgeVisited = new HashMap<>();
            solutionEdges.forEach(e -> edgeVisited.put(e, Boolean.FALSE));

            dfs(flow.getIngressNode(), edgeVisited, GraphUtil.getEdgesBySource(solutionEdges));

            for (TopologyEdge edge : edgeVisited.keySet()) {
                if (!edgeVisited.get(edge)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void dfs(TopologyVertex current, Map<TopologyEdge, Boolean> visited, Map<TopologyVertex, List<TopologyEdge>> edgesBySrc) {
        List<TopologyEdge> edgesFromCurrent = edgesBySrc.get(current);
        if (edgesFromCurrent == null) {
            return;
        }
        for (TopologyEdge edge : edgesBySrc.get(current)) {
            visited.put(edge, Boolean.TRUE);
            dfs(edge.dst(), visited, edgesBySrc);
        }
        return;
    }

    /**
     * Checks if every destination is reachable in the tree. Does this by performing a DFS on the tree and check if
     * every destination node is found.
     *
     * @param sol the solution to test
     * @return true if all destinations for each flow are reachable from the source, false otherwise
     */
    public boolean destinationsReachable(NfvPlacementSolution sol) {
        for (NprTraffic flow : sol.getRequest().getTraffic()) {
            Set<TopologyEdge> edges = sol.getSolutionEdgesByTraffic(flow);

            Map<TopologyVertex, Boolean> visitedMap = new HashMap<>();
            for (TopologyEdge edge : edges) {
                visitedMap.put(edge.src(), Boolean.FALSE);
                visitedMap.put(edge.dst(), Boolean.FALSE);
            }

            Map<TopologyVertex, List<TopologyEdge>> edgesBySrc = GraphUtil.getEdgesBySource(edges);

            visitedMap.put(flow.getIngressNode(), Boolean.TRUE);
            for (TopologyEdge edge : edgesBySrc.get(flow.getIngressNode())) {
                dfsUnnecessary(edge.dst(), visitedMap, edgesBySrc, false);
            }
            boolean allDstsReached = true;
            for (TopologyVertex dst : flow.getEgressNodes()) {
                allDstsReached &= visitedMap.get(dst);
            }
            if (!allDstsReached) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if there are unnecessary edges in the tree, i.e. edges which can be removed without destroying the
     * property of the tree that all destinations can be reached. The check is performed by a DFS. Unnecessary
     * edges/loops are detected if there is an edge which leads to an node which has been already found by the DFS.
     *
     * @param traffic the traffic for which the solution is tested
     * @param edges   the edges of the tree
     * @return true if there are unneccessary edges in the tree, false otherwise
     */
    protected boolean containsUnnecessaryEdges(NprTraffic traffic, Set<TopologyEdge> edges) {
        Map<TopologyVertex, Boolean> visitedMap = new HashMap<>();
        for (TopologyEdge edge : edges) {
            visitedMap.put(edge.src(), Boolean.FALSE);
            visitedMap.put(edge.dst(), Boolean.FALSE);
        }

        Map<TopologyVertex, List<TopologyEdge>> edgesBySrc = new HashMap<>();
        edges.forEach(e -> {
            TopologyVertex src = e.src();
            edgesBySrc.computeIfAbsent(src, k -> new ArrayList<>());
            edgesBySrc.get(e.src()).add(e);
        });

        visitedMap.put(traffic.getIngressNode(), Boolean.TRUE);
        for (TopologyEdge edge : edgesBySrc.get(traffic.getIngressNode())) {
            if (dfsUnnecessary(edge.dst(), visitedMap, edgesBySrc, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs a depth first search, marks visited nodes and returns whether or not edges were found which lead to a
     * node which has already been marked. These edges are considered unnecessary.
     *
     * @param current             the vertex which is currently explored
     * @param visited             contains which vertexes has been visited already
     * @param edgesBySrc          the edges to explore by source vertex
     * @param unnecessaryDetected whether unnecessary edges (i.e., edges which lead to an already explored vertex) have
     *                            been detected yet
     * @return true if unnecessary edges have been detected, false otherwise
     */
    private boolean dfsUnnecessary(TopologyVertex current, Map<TopologyVertex, Boolean> visited, Map<TopologyVertex, List<TopologyEdge>> edgesBySrc, boolean unnecessaryDetected) {
        visited.put(current, Boolean.TRUE);
        List<TopologyEdge> edgesFromCurrent = edgesBySrc.get(current);
        if (edgesFromCurrent == null) {
            return unnecessaryDetected;
        }
        for (TopologyEdge edge : edgesBySrc.get(current)) {
            TopologyVertex edgeDst = edge.dst();
            if (visited.get(edgeDst)) {
                unnecessaryDetected = true;
                continue;
            }
            dfsUnnecessary(edgeDst, visited, edgesBySrc, unnecessaryDetected);
        }
        return unnecessaryDetected;
    }
}
