package thesiscode.common.util;

import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class ScaleDemandGenerator implements IDemandGenerator {
    private Set<TopologyVertex> vertices;
    private int amountOfDemands;
    private int egressPerDemand;
    private int sfcLength;
    private int bandwidthDemand;

    public ScaleDemandGenerator(Set<TopologyVertex> vertices, int amountOfDemands, int egressPerDemand, int sfcLength, int bandwidthDemandPerFlow) {
        this.vertices = vertices;
        this.amountOfDemands = amountOfDemands;
        this.egressPerDemand = egressPerDemand;
        this.sfcLength = sfcLength;
        this.bandwidthDemand = bandwidthDemandPerFlow;
    }

    @Override
    public List<NprTraffic> generateDemand() {
        List<NprTraffic> demand = new ArrayList<>();
        for (int i = 0; i < amountOfDemands; i++) {
            Set<TopologyVertex> vertexSet = new HashSet<>(vertices);
            TopologyVertex[] vertArr = vertexSet.toArray(new TopologyVertex[0]);

            TopologyVertex ingress = randomIngress(vertArr);

            vertexSet.remove(ingress);

            Set<TopologyVertex> egress = randomEgress(vertexSet);

            List<NprNfvTypes.Type> sfc;
            if (sfcLength == 0) {
                sfc = new ArrayList<>();
            } else {
                sfc = randomSfc();
            }


            demand.add(new NprTraffic(sfc, ingress, egress, bandwidthDemand));
        }
        return demand;
    }

    private List<NprNfvTypes.Type> randomSfc() {
        NprNfvTypes.Type[] allTypes = NprNfvTypes.Type.values();
        Set<NprNfvTypes.Type> typeSet = new HashSet<>(Arrays.asList(allTypes));

        if (typeSet.size() < sfcLength) {
            throw new IllegalStateException(
                    "Cannot choose " + sfcLength + " distinct VNFs from a set of " + typeSet.size() + "VNF types");
        }

        List<NprNfvTypes.Type> chosenTypes = new ArrayList<>();
        for (int i = 0; i < sfcLength; i++) {
            int index = ThreadLocalRandom.current().nextInt(allTypes.length);
            NprNfvTypes.Type chosenType = allTypes[index];
            chosenTypes.add(chosenType);
            typeSet.remove(chosenType);
            allTypes = typeSet.toArray(new NprNfvTypes.Type[0]);
        }

        return chosenTypes;
    }

    private TopologyVertex randomIngress(TopologyVertex[] availableVertexes) {
        int index = ThreadLocalRandom.current().nextInt(availableVertexes.length);
        return availableVertexes[index];
    }

    private Set<TopologyVertex> randomEgress(Set<TopologyVertex> availableVertexes) {
        if (egressPerDemand > availableVertexes.size()) {
            throw new IllegalStateException(
                    "Cannot choose " + egressPerDemand + "egress nodes from a set of " + availableVertexes.size() +
                            "nodes...");
        }
        TopologyVertex[] availVertArr = availableVertexes.toArray(new TopologyVertex[0]);
        Set<TopologyVertex> vertices = new HashSet<>();


        for (int i = 0; i < egressPerDemand; i++) {
            int index = ThreadLocalRandom.current().nextInt(availVertArr.length);
            TopologyVertex chosenEgress = availVertArr[index];
            vertices.add(chosenEgress);
            availableVertexes.remove(chosenEgress);
            availVertArr = availableVertexes.toArray(new TopologyVertex[0]);
        }

        return vertices;
    }
}
