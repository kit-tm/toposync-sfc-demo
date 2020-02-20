package main.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.placement.solver.NfvPlacementSolution;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;

import java.util.*;

public class SolutionJsonEncoder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public String toJson(NfvPlacementSolution solution) {
        if (solution == null) {
            return null;
        }

        final Set<TopologyVertex> placements = Objects.requireNonNull(placements(solution));
        final List<Set<TopologyEdge>> edges = Objects.requireNonNull(logicalEdges(solution));

        JSONArray placementsJson = placementsJson(placements);
        JSONArray edgesJson = edgesJson(edges);


        return new JSONObject().put("solution", new JSONObject().put("placement", placementsJson)
                                                                .put("edges", edgesJson)).toString();
    }

    private JSONArray placementsJson(Set<TopologyVertex> placements) {
        JSONArray jsonArray = new JSONArray();

        for (TopologyVertex node : placements) {
            jsonArray.put(node.deviceId().toString());
        }

        return jsonArray;
    }

    private JSONArray edgesJson(List<Set<TopologyEdge>> edges) {
        JSONArray jsonArray = new JSONArray();

        for (Set<TopologyEdge> edgesPerLogical : edges) {
            JSONArray arrayPerLogical = new JSONArray();

            for (TopologyEdge edge : edgesPerLogical) {
                JSONObject edgeObject = new JSONObject();

                edgeObject.put("src", edge.src().deviceId().toString());
                edgeObject.put("dst", edge.dst().deviceId().toString());

                arrayPerLogical.put(edgeObject);
            }

            jsonArray.put(arrayPerLogical);
        }

        return jsonArray;
    }

    private Set<TopologyVertex> placements(NfvPlacementSolution solution) {
        final Map<NprNfvTypes.Type, Set<TopologyVertex>> placements = solution.getSharedPlacements();

        if (placements.size() == 1) {
            for (Map.Entry<NprNfvTypes.Type, Set<TopologyVertex>> entry : placements.entrySet()) {
                return entry.getValue();
            }
        } else {
            logger.warn("found more than one VNF Type, this shouldn't have happened: {}", placements.keySet());
        }
        return null;
    }

    private List<Set<TopologyEdge>> logicalEdges(NfvPlacementSolution solution) {
        final Map<NprTraffic, List<Set<TopologyEdge>>> edges = solution.getLogicalEdgesPerTraffic();
        if (edges.size() == 1) {
            for (Map.Entry<NprTraffic, List<Set<TopologyEdge>>> entry : edges.entrySet()) {
                return entry.getValue();
            }
        } else {
            logger.warn("found more than one traffic, this shouldn't have happened: {}", edges.keySet());
        }
        return null;
    }

}
