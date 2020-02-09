package thesiscode.common.util;

import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprTraffic;
import thesiscode.common.topo.ILinkWeigher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RandomDemandGenerator implements IDemandGenerator {
    private TopologyGraph graph;
    private ILinkWeigher weigher;
    private boolean hasSfc = false;

    public RandomDemandGenerator(TopologyGraph topo, ILinkWeigher weigher) {
        this.graph = topo;
        this.weigher = weigher;
    }

    public RandomDemandGenerator(TopologyGraph topo, ILinkWeigher weigher, boolean hasSfc) {
        this(topo, weigher);
        this.hasSfc = hasSfc;
    }

    public List<NprTraffic> generateDemand() {
        List<NprTraffic> generatedTraffic = new ArrayList<>();
        Random rand = new Random();


        int remainingCapacity = findMinimumCapacity();

        while (remainingCapacity > 0) {
            Set<TopologyVertex> vertexSet = new HashSet<TopologyVertex>(graph.getVertexes());
            int amountOfVertexes = vertexSet.size();
            TopologyVertex[] vertArr = vertexSet.toArray(new TopologyVertex[0]);

            // choose ingress node
            int ingressIndex = rand.nextInt(amountOfVertexes);
            TopologyVertex ingress = vertArr[ingressIndex];

            // remove ingress from possible egress (we do not want ingress and egress to overlap)
            vertexSet.remove(ingress);
            vertArr = vertexSet.toArray(new TopologyVertex[0]);

            // choose egress nodes
            Set<TopologyVertex> egressVerts = new HashSet<>();
            int amountOfEgress = 0;
            while (amountOfEgress == 0) { // at minimum 1 egress node, if 0 was chosen by random, roll the dice again
                amountOfEgress = rand.nextInt(amountOfVertexes - 1);// up to |V|-1 egress nodes
            }
            while (amountOfEgress > 0) {
                TopologyVertex egress = vertArr[rand.nextInt(vertArr.length)];
                egressVerts.add(egress);
                vertexSet.remove(egress);
                vertArr = vertexSet.toArray(new TopologyVertex[0]);
                amountOfEgress--;
            }


            // choose demand > 0 but <= remainingCapacity
            int demand = 0;
            while (demand == 0) {
                demand = rand.nextInt(remainingCapacity + 1);
            }
            remainingCapacity -= demand;

            List<NprNfvTypes.Type> sfc = new ArrayList<>();
            if (hasSfc) {
                for (int i = 0; i < ThreadLocalRandom.current().nextInt(1, 4); i++) {
                    switch (ThreadLocalRandom.current().nextInt(0, 4)) {
                        case 0:
                            addVnfIfNotContained(sfc, NprNfvTypes.Type.TRANSCODER);
                            break;
                        case 1:
                            addVnfIfNotContained(sfc, NprNfvTypes.Type.FIREWALL);
                            break;
                        case 2:
                            addVnfIfNotContained(sfc, NprNfvTypes.Type.INTRUSION_DETECTION);
                            break;
                        case 3:
                            // intentionally blank, in this case no VNF is added to the SFC
                            break;
                        default:
                            throw new IllegalStateException("unexpected random when creating sfc");
                    }
                }
            }
            generatedTraffic.add(new NprTraffic(sfc, ingress, egressVerts, demand));
        }

        return generatedTraffic;
    }

    private void addVnfIfNotContained(List<NprNfvTypes.Type> sfc, NprNfvTypes.Type toAdd) {
        if (!sfc.contains(toAdd)) {
            sfc.add(toAdd);
        }
    }


    private int findMinimumCapacity() {
        int minimumCapacity = Integer.MAX_VALUE;
        for (TopologyEdge edge : graph.getEdges()) {
            double weight = weigher.getBandwidth(edge);
            if (weight < minimumCapacity) {
                minimumCapacity = (int) weight;
            }
        }
        return minimumCapacity;
    }
}
