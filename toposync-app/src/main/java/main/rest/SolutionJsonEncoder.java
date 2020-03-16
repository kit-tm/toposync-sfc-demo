package main.rest;

import main.RequestGenerator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thesiscode.common.nfv.placement.solver.NfvPlacementSolution;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SolutionJsonEncoder {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private JSONObject json;
    private NfvPlacementSolution solution;

    public String toJson(NfvPlacementSolution solution) {
        if (solution == null) {
            return null;
        }
        this.solution = solution;

        json = new JSONObject();

        putDeviation();
        putDelays();

        putSolution();

        return json.toString();
    }

    private void putDeviation() {
        final double delayDeviation = solution.getDeviationSum();
        json.put("deviation", delayDeviation / RequestGenerator.DELAY);
    }

    private void putDelays() {
        JSONObject delaysJson = new JSONObject();
        final List<NprTraffic> requestFlows = solution.getRequest().getTraffic();
        if (requestFlows.size() != 1) {
            throw new IllegalStateException("Unexpected request list size: " + requestFlows.size());
        }
        final NprTraffic traffic = solution.getRequest().getTraffic().get(0);
        Map<TopologyVertex, Double> delays = solution.getDelaysOfFlow(traffic);
        for (TopologyVertex dest : traffic.getEgressNodes()) {
            final double delayWithoutProcessingDelay =
                    delays.get(dest) - NprNfvTypes.getBaseDelay(NprNfvTypes.Type.TRANSCODER);
            final double normalizedDelay = delayWithoutProcessingDelay / RequestGenerator.DELAY + 2; // +2 for VNF
            delaysJson.put(dest.deviceId().toString(), normalizedDelay);
        }
        json.put("delays", delaysJson);
    }

    private void putSolution() {
        JSONObject solutionJson = new JSONObject();

        final Set<TopologyVertex> placements = Objects.requireNonNull(placements());
        JSONArray placementsJson = placementsJson(placements);
        solutionJson.put("placement", placementsJson);

        final List<Set<TopologyEdge>> edges = Objects.requireNonNull(logicalEdges());
        JSONArray edgesJson = edgesJson(edges);
        solutionJson.put("edges", edgesJson);

        json.put("solution", solutionJson);
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

    private Set<TopologyVertex> placements() {
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

    private List<Set<TopologyEdge>> logicalEdges() {
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
