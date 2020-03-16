package thesiscode.common.nfv.placement.solver;

import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class NfvPlacementSolution {
    private Map<NprTraffic, Set<TopologyEdge>> edgesPerTraffic;
    private Map<NprTraffic, Map<NprNfvTypes.Type, Set<TopologyVertex>>> placements;
    private Map<NprNfvTypes.Type, Set<TopologyVertex>> sharedPlacements;
    private NfvPlacementRequest request;
    private OptimizationGoal goal;
    private double value;
    private double deviationSum;
    private double delaySum;
    private double networkLoad;
    private Map<NprTraffic, Double> maxDelayPerFlow;
    private Map<NprTraffic, Double> delayDeviationPerFlow;
    private Map<NprTraffic, List<Set<TopologyEdge>>> logicalEdgesPerTraffic;
    private Map<NprTraffic, Map<TopologyVertex, Double>> delays;

    public NfvPlacementSolution(Map<NprTraffic, Set<TopologyEdge>> edgesPerTraffic, Map<NprTraffic,
            Map<NprNfvTypes.Type, Set<TopologyVertex>>> placements, NfvPlacementRequest request, double deviationSum,
                                double delaySum, double networkLoad) {
        this.edgesPerTraffic = edgesPerTraffic;
        this.placements = placements;
        this.request = request;
        this.deviationSum = deviationSum;
        this.delaySum = delaySum;
        this.networkLoad = networkLoad;

        logicalEdgesPerTraffic = new HashMap<>();

        sharedPlacements = new HashMap<>();
        for (NprTraffic traffic : placements.keySet()) {
            Map<NprNfvTypes.Type, Set<TopologyVertex>> placementForTraffic = getPlacementsForTraffic(traffic);

            for (NprNfvTypes.Type type : placementForTraffic.keySet()) {
                sharedPlacements.computeIfAbsent(type, t -> new HashSet<>());
                sharedPlacements.get(type).addAll(placementForTraffic.get(type));
            }
        }
    }


    public void setLogicalEdgesForTraffic(NprTraffic traffic, List<Set<TopologyEdge>> logicalToRealEdges) {
        logicalEdgesPerTraffic.put(traffic, logicalToRealEdges);
    }

    public Map<NprTraffic, List<Set<TopologyEdge>>> getLogicalEdgesPerTraffic() {
        return logicalEdgesPerTraffic;
    }

    public double getDeviationSum() {
        return deviationSum;
    }

    public double getDelaySum() {
        return delaySum;
    }

    public double getNetworkLoad() {
        return networkLoad;
    }

    public NfvPlacementRequest getRequest() {
        return request;
    }

    public void setDelays(Map<NprTraffic, Map<TopologyVertex, Double>> delays) {
        this.delays = delays;
    }

    public NfvPlacementSolution(Map<NprTraffic, Set<TopologyEdge>> edgesPerTraffic, Map<NprTraffic,
            Map<NprNfvTypes.Type, Set<TopologyVertex>>> placements, NfvPlacementRequest request,
                                OptimizationGoal goal, double value, double deviationSum, double delaySum,
                                double networkLoad, Map<NprTraffic, Double> deviationPerTraffic, Map<NprTraffic,
            Double> maxDelayPerTraffic) {
        this(edgesPerTraffic, placements, request, deviationSum, delaySum, networkLoad);
        this.goal = goal;
        this.value = value;
        this.delayDeviationPerFlow = deviationPerTraffic;
        this.maxDelayPerFlow = maxDelayPerTraffic;
    }

    public Map<NprTraffic, Set<TopologyEdge>> getSolutionEdges() {
        return edgesPerTraffic;
    }

    public Set<TopologyEdge> getSolutionEdgesByTraffic(NprTraffic traffic) {
        return edgesPerTraffic.get(traffic);
    }


    public Map<NprNfvTypes.Type, Set<TopologyVertex>> getPlacementsForTraffic(NprTraffic traffic) {
        return placements.get(traffic);
    }

    public Map<NprNfvTypes.Type, Set<TopologyVertex>> getSharedPlacements() {
        return sharedPlacements;
    }

    public double getMaxDelayOfFlow(NprTraffic flow) {
        return maxDelayPerFlow.get(flow);
    }

    public Map<TopologyVertex, Double> getDelaysOfFlow(NprTraffic flow) {
        return delays.get(flow);
    }

    public double getDelayDeviationOfFlow(NprTraffic flow) {
        return delayDeviationPerFlow.get(flow);
    }

    public double getValue() {
        return value;
    }

    /**
     * This method is used for testing/debugging/visualization purposes.
     * Writes this solution to a csv file in the following format (spaces for readability):
     * file_content := objectiveFunction, objectiveValue \n [traffic\n]
     * traffic := demand \n placement \n edges
     * demand := demandValue, sourceDeviceId [,destinationDeviceId]\n
     * placement := [TPYE@deviceId,] \n or NO \n
     * edges := [edge\n]
     * edge := edgeSourceDevId, edgeDestinationDevId\n
     *
     * @param filePath the path of the file
     * @throws FileNotFoundException if an error occured during creation/location of the file
     */
    @Deprecated
    public void writeToCsv(String filePath) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new File(filePath));

        // objectiveFunction,objectiveValue\n
        pw.write(goal.toString() + ',' + value + '\n');

        StringBuilder sb = new StringBuilder();

        //assert edgesPerTraffic.keySet().equals(placements.keySet());

        for (NprTraffic traffic : edgesPerTraffic.keySet()) {
            // demand\n
            sb.append(traffic.getDemand()).append(',');
            sb.append(traffic.getIngressNode().deviceId().toString());
            traffic.getEgressNodes().forEach(e -> sb.append(',').append(e.deviceId().toString()));
            sb.append('\n');

            if (traffic.getSfc().size() > 0) {
                // placement\n
                Map<NprNfvTypes.Type, Set<TopologyVertex>> placementForTraffic = placements.get(traffic);
                boolean first = true;
                for (NprNfvTypes.Type type : placementForTraffic.keySet()) {
                    if (!first) {
                        sb.append(',');
                    }
                    first = false;

                    for (TopologyVertex placedAt : placementForTraffic.get(type)) {
                        sb.append(type.name()).append('@').append(placedAt.toString());
                    }
                }
            } else {
                sb.append("NO"); // no placement
            }

            sb.append('\n');

            // edges
            edgesPerTraffic.get(traffic).forEach(e -> {
                sb.append(e.src().deviceId().toString()).append(',').append(e.dst().deviceId().toString()).append('\n');
            });

            // separating traffics
            sb.append('\n');
        }


        pw.write(sb.toString());
        pw.close();

        System.out.println("wrote solution to csv");
    }


}
