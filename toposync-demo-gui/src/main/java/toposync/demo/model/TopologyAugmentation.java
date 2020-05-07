package toposync.demo.model;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class TopologyAugmentation {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Graph topology;
    private Graph solution;

    /**
     * Merges the solution into the topo. Therefore first removes the old solution from the topo.
     *
     * @param topo the topology
     * @param sol  the solution
     */
    public void merge(Graph topo, Graph sol) {
        Objects.requireNonNull(topo);

        this.topology = topo;
        this.solution = sol;

        removeOldSolution();
        mergeInternal();
    }

    private void removeOldSolution() {
        removeOldEdges();
        removeOldNodes();
    }

    private void removeOldNodes() {
        for (Node node : topology) {
            if (node.getId().equals("vnf")) {
                topology.removeNode(node);
            }
        }
    }

    private void removeOldEdges() {
        for (Node n : topology) {
            for (Edge e : n) {
                removeEdgeIfSolutionEdge(e);
            }
        }

        for (int i = 0; i < topology.getEdgeCount(); i++) {
            Edge e = topology.getEdge(i);
            removeEdgeIfSolutionEdge(e);
        }
    }

    private void removeEdgeIfSolutionEdge(Edge e) {
        if (e != null) {
            final String id = e.getId();
            if (id.startsWith("solEdge") || id.equals("vnf->pop") || id.equals("pop->vnf")) {
                topology.removeEdge(e);
                logger.debug("removed old edge: {}", e);
            }
        }
    }

    private void mergeInternal() {
        if (solution != null) {
            copyEdgesFromSolution();
            copyUiClassFromSolutionNodes();
            copyUiLabelFromSolutionNodes();
        }
    }

    private void copyUiLabelFromSolutionNodes() {
        for (Node topoNode : solution) {
            final String uiLabel = (String) topoNode.getAttribute("ui.label");
            if (uiLabel != null) {
                topology.getNode(topoNode.getId()).setAttribute("ui.label", uiLabel);
            }
        }
    }

    private void copyUiClassFromSolutionNodes() {
        for (Node topoNode : solution) {
            final String uiClass = (String) topoNode.getAttribute("ui.class");
            if (uiClass != null) {
                topology.getNode(topoNode.getId()).setAttribute("ui.class", uiClass);
            }
        }
    }

    private void copyEdgesFromSolution() {
        solution.edges().forEach(e -> {
            final String edgeId = e.getId();
            final String node0Id = e.getNode0().getId();
            final String node1Id = e.getNode1().getId();

            Edge addedEdge = topology.addEdge(edgeId, node0Id, node1Id, true);
            addedEdge.setAttribute("ui.class", e.getAttribute("ui.class"));
        });
    }
}
