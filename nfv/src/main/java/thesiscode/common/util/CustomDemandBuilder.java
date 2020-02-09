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

/**
 * Is a builder for a list of {@link NprTraffic}. Usage pattern: Create an instance of this class. Call
 * {@link #setIngress(TopologyVertex)} to set the ingress node. Call {@link #setEgress(Set)} and/or
 * {@link #addEgress(TopologyVertex)} to create the egress set.
 */
public class CustomDemandBuilder implements IDemandGenerator {

    private TopologyVertex currentIngress;
    private Set<TopologyVertex> currentEgress;
    private List<NprNfvTypes.Type> currentSfc;
    private int currentDemandValue;

    private List<NprTraffic> generatedDemand = new ArrayList<>();

    public CustomDemandBuilder setIngress(TopologyVertex ingress) {
        this.currentIngress = ingress;
        return this;
    }

    public CustomDemandBuilder setEgress(Set<TopologyVertex> egress) {
        this.currentEgress = egress;
        return this;
    }

    public CustomDemandBuilder addEgress(TopologyVertex egressToAdd) {
        if (currentEgress == null) {
            currentEgress = new HashSet<>();
        }
        currentEgress.add(egressToAdd);
        return this;
    }

    public CustomDemandBuilder setRandomIngressAndEgress(int egressAmount, Set<TopologyVertex> availableVertices) {
        if (egressAmount + 1 > availableVertices.size()) {
            throw new IllegalStateException(
                    "Cannot choose " + egressAmount + "egress nodes and a ingress node from a set of " +
                            availableVertices.size() + "nodes...");
        }

        // copy the set
        Set<TopologyVertex> copySet = new HashSet<>(availableVertices);


        // choose random ingress
        TopologyVertex[] availVertArr = copySet.toArray(new TopologyVertex[0]);
        int index = ThreadLocalRandom.current().nextInt(availVertArr.length);
        currentIngress = availVertArr[index];
        copySet.remove(currentIngress);
        availVertArr = copySet.toArray(new TopologyVertex[0]);

        // choose random egress
        currentEgress = new HashSet<>();
        while (currentEgress.size() < egressAmount) {
            index = ThreadLocalRandom.current().nextInt(availVertArr.length);
            TopologyVertex chosenEgress = availVertArr[index];
            if (chosenEgress.deviceId().toString().startsWith("tbs")) {
                currentEgress.add(chosenEgress);
            }
            copySet.remove(chosenEgress);
            availVertArr = copySet.toArray(new TopologyVertex[0]);
        }


        return this;
    }

    public CustomDemandBuilder setSfc(List<NprNfvTypes.Type> sfc) {
        currentSfc = sfc;
        return this;
    }

    public CustomDemandBuilder setRandomSfc(int sfcLength) {
        NprNfvTypes.Type[] allTypes = NprNfvTypes.Type.values();
        Set<NprNfvTypes.Type> typeSet = new HashSet<>(Arrays.asList(allTypes));

        if (sfcLength > typeSet.size()) {
            throw new IllegalStateException(
                    "Cannot create an SFC of length " + sfcLength + " with only " + NprNfvTypes.Type.values().length +
                            " available VNF types");
        }

        currentSfc = new ArrayList<>();
        for (int i = 0; i < sfcLength; i++) {
            int index = ThreadLocalRandom.current().nextInt(allTypes.length);
            NprNfvTypes.Type chosenType = allTypes[index];
            currentSfc.add(chosenType);
            typeSet.remove(chosenType);
            allTypes = typeSet.toArray(new NprNfvTypes.Type[0]);
        }

        return this;
    }

    public CustomDemandBuilder setDemandValue(int demandValue) {
        currentDemandValue = demandValue;
        return this;
    }


    public CustomDemandBuilder createDemand() {
        if (currentIngress == null) {
            throw new IllegalStateException("no ingress vertex was set");
        }
        if (currentEgress == null) {
            throw new IllegalStateException("no egress nodes were set");
        }
        if (currentDemandValue == 0) {
            throw new IllegalStateException("demand value must not be 0");
        }
        if (currentSfc == null) {
            throw new IllegalStateException("no sfc was set");
        }

        generatedDemand.add(new NprTraffic(currentSfc, currentIngress, currentEgress, currentDemandValue));

        resetBuildState();

        return this;
    }

    private void resetBuildState() {
        currentIngress = null;
        currentEgress = null;
        currentDemandValue = 0;
        currentSfc = null;
    }


    @Override
    public List<NprTraffic> generateDemand() {
        return generatedDemand;
    }
}
