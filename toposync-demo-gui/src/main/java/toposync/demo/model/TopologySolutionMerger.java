package toposync.demo.model;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.MultiGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class TopologySolutionMerger {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Graph topology;
    private Graph solution;
    private Graph merged;

    public Graph merge(Graph topo, Graph sol) {
        Objects.requireNonNull(topo);
        Objects.requireNonNull(sol);

        this.topology = topo;
        this.solution = sol;
        merged = new MultiGraph("Merged topo and tree");
        merged.setStrict(false);
        merged.setAutoCreate(true);

        mergeInternal();

        return merged;
    }

    private void mergeInternal() {
        copyEdgesFromTopo();
        copyUiClassFromTopoNodes();

        copyEdgesFromSolution();
        copyUiClassFromSolutionNodes();
    }

    private void copyEdgesFromTopo() {
        topology.edges().forEach(e -> {
            merged.addEdge(e.getId(), e.getNode0().getId(), e.getNode1().getId(), false);
        });
    }

    private void copyUiClassFromTopoNodes() {
        copyUiClass(topology);
    }

    private void copyUiClass(Graph toCopyFrom) {
        for (Node topoNode : toCopyFrom) {
            final String uiClass = (String) topoNode.getAttribute("ui.class");
            if (uiClass != null) {
                merged.getNode(topoNode.getId()).setAttribute("ui.class", uiClass);
            }
        }
    }

    private void copyEdgesFromSolution() {
        solution.edges().forEach(e -> {
            final String edgeId = e.getId();
            final String node0Id = e.getNode0().getId();
            final String node1Id = e.getNode1().getId();

            Edge addedEdge = merged.addEdge(edgeId, node0Id, node1Id, true);
            logger.info("ui class {}", e.getAttribute("ui.class"));
            addedEdge.setAttribute("ui.class", e.getAttribute("ui.class"));
        });
    }

    private void copyUiClassFromSolutionNodes() {
        copyUiClass(solution);
    }
}
