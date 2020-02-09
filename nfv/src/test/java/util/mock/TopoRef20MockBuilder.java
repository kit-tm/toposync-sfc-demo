package util.mock;

import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprResources;
import thesiscode.common.topo.WrappedPoPVertex;
import thesiscode.common.topo.WrappedVertex;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class TopoRef20MockBuilder {

    public static TestGraph getMockedTopo() {
        TestGraph tg = new TestGraph(new LinkedHashSet<>(), new LinkedHashSet<>());

        TopologyVertex[] vertices = new TopologyVertex[8];

        Map<NprNfvTypes.Type, Integer> deploymentCost = new HashMap<>();

        for (NprNfvTypes.Type type : NprNfvTypes.Type.values()) {
            deploymentCost.put(type, 5);
        }

        Map<NprNfvTypes.Type, Double> hwAccel = new HashMap<>();
        for (NprNfvTypes.Type type : NprNfvTypes.Type.values()) {
            hwAccel.put(type, 1.0);
        }

        Map<NprResources, Integer> resourceCapacity = new HashMap<>();
        resourceCapacity.put(NprResources.CPU_CORES, 6);
        resourceCapacity.put(NprResources.RAM_IN_GB, 2);


        for (int i = 0; i < 8; i++) {
            if (i < 2) {
                vertices[i] = new WrappedVertex(new DefaultTopologyVertex(DeviceId.deviceId("s" + (i + 1))));
            } else {
                vertices[i] = new WrappedPoPVertex(new DefaultTopologyVertex(DeviceId.deviceId(
                        "s" + (i + 1))), deploymentCost, hwAccel, resourceCapacity);
            }

            tg.addVertex(vertices[i], NodeType.NONE);
        }

        tg.addDirectionalEdgesBetween(vertices[0], vertices[1]);
        tg.addDirectionalEdgesBetween(vertices[0], vertices[2]);
        tg.addDirectionalEdgesBetween(vertices[1], vertices[3]);
        tg.addDirectionalEdgesBetween(vertices[3], vertices[5]);
        tg.addDirectionalEdgesBetween(vertices[4], vertices[5]);
        tg.addDirectionalEdgesBetween(vertices[4], vertices[7]);
        tg.addDirectionalEdgesBetween(vertices[6], vertices[7]);
        tg.addDirectionalEdgesBetween(vertices[2], vertices[6]);
        tg.addDirectionalEdgesBetween(vertices[2], vertices[3]);
        tg.addDirectionalEdgesBetween(vertices[3], vertices[4]);
        tg.addDirectionalEdgesBetween(vertices[2], vertices[4]);
        tg.addDirectionalEdgesBetween(vertices[4], vertices[6]);

        return tg;
    }

}
