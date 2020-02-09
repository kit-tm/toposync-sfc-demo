package util.mock;

import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.nfv.traffic.NprNfvTypes;
import thesiscode.common.nfv.traffic.NprResources;
import thesiscode.common.topo.WrappedPoPVertex;
import thesiscode.common.topo.WrappedVertex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class EvalTopoMockBuilder {
    public static TestGraph getMockedTopo() {
        TestGraph tg = TopoRef20MockBuilder.getMockedTopo();

        TopologyVertex s9 = new WrappedVertex(new DefaultTopologyVertex(DeviceId.deviceId("s9")));
        TopologyVertex s10 = new WrappedVertex(new DefaultTopologyVertex(DeviceId.deviceId("s10")));

        tg.addVertex(s9, NodeType.NONE);
        tg.addVertex(s10, NodeType.NONE);

        TopologyVertex s6 = tg.getById(devId("s6"));
        TopologyVertex s8 = tg.getById(devId("s8"));

        tg.addDirectionalEdgesBetween(s6, s9);
        tg.addDirectionalEdgesBetween(s8, s10);

        return tg;
    }

    public static TestGraph getEvalTopo() {
        TestGraph tg = new TestGraph(new HashSet<>(), new HashSet<>());


        // add DXTTs
        TopologyVertex[] dxtt = generateWrappedVertexArray(4, "dxtt", true, tg, NodeType.DXTT);

        // add fully meshed network between DXTTs
        for (int i = 0; i < dxtt.length; i++) {
            for (int j = i + 1; j < dxtt.length; j++) {
                tg.addDirectionalEdgesBetween(dxtt[i], dxtt[j]);
            }
        }

        // add DXTs

        // DXT1*
        TopologyVertex[] dxt1 = generateWrappedVertexArray(2, "dxt1", true, tg, NodeType.DXT);
        tg.addDirectionalEdgesBetween(dxt1[0], dxtt[0]);
        tg.addDirectionalEdgesBetween(dxt1[0], dxtt[1]);
        tg.addDirectionalEdgesBetween(dxt1[1], dxtt[0]);
        tg.addDirectionalEdgesBetween(dxt1[1], dxtt[2]);

        // DXT2*
        TopologyVertex[] dxt2 = generateWrappedVertexArray(3, "dxt2", true, tg, NodeType.DXT);
        tg.addDirectionalEdgesBetween(dxt2[0], dxtt[0]);
        tg.addDirectionalEdgesBetween(dxt2[0], dxtt[1]);
        tg.addDirectionalEdgesBetween(dxt2[1], dxtt[1]);
        tg.addDirectionalEdgesBetween(dxt2[1], dxtt[3]);
        tg.addDirectionalEdgesBetween(dxt2[2], dxtt[1]);
        tg.addDirectionalEdgesBetween(dxt2[2], dxtt[3]);

        // DXT3*
        TopologyVertex[] dxt3 = generateWrappedVertexArray(3, "dxt3", true, tg, NodeType.DXT);
        tg.addDirectionalEdgesBetween(dxt3[0], dxtt[0]);
        tg.addDirectionalEdgesBetween(dxt3[0], dxtt[2]);
        tg.addDirectionalEdgesBetween(dxt3[1], dxtt[0]);
        tg.addDirectionalEdgesBetween(dxt3[1], dxtt[2]);
        tg.addDirectionalEdgesBetween(dxt3[2], dxtt[2]);
        tg.addDirectionalEdgesBetween(dxt3[2], dxtt[3]);

        // DXT4*
        TopologyVertex[] dxt4 = generateWrappedVertexArray(2, "dxt4", true, tg, NodeType.DXT);
        tg.addDirectionalEdgesBetween(dxt4[0], dxtt[3]);
        tg.addDirectionalEdgesBetween(dxt4[0], dxtt[1]);
        tg.addDirectionalEdgesBetween(dxt4[1], dxtt[3]);
        tg.addDirectionalEdgesBetween(dxt4[1], dxtt[2]);

        // add TBS
        TopologyVertex tbs11[] = generateWrappedVertexArray(2, "tbs11", false, tg, NodeType.TBS);
        addStar(dxt1[0], tbs11, tg);

        TopologyVertex tbs12[] = generateWrappedVertexArray(3, "tbs12", false, tg, NodeType.TBS);
        addStar(dxt1[1], tbs12, tg);

        TopologyVertex tbs21[] = generateWrappedVertexArray(3, "tbs21", false, tg, NodeType.TBS);
        addStar(dxt2[0], tbs21, tg);

        TopologyVertex tbs22[] = generateWrappedVertexArray(2, "tbs22", false, tg, NodeType.TBS);
        addRing(dxt2[1], tbs22, tg);

        TopologyVertex tbs23[] = generateWrappedVertexArray(2, "tbs23", false, tg, NodeType.TBS);
        addStar(dxt2[2], tbs23, tg);

        TopologyVertex tbs31[] = generateWrappedVertexArray(3, "tbs31", false, tg, NodeType.TBS);
        addStar(dxt3[0], tbs31, tg);

        TopologyVertex tbs32[] = generateWrappedVertexArray(2, "tbs32", false, tg, NodeType.TBS);
        addRing(dxt3[1], tbs32, tg);

        TopologyVertex tbs33[] = generateWrappedVertexArray(3, "tbs33", false, tg, NodeType.TBS);
        addRing(dxt3[2], tbs33, tg);

        TopologyVertex tbs41[] = generateWrappedVertexArray(2, "tbs41", false, tg, NodeType.TBS);
        addStar(dxt4[0], tbs41, tg);

        TopologyVertex tbs42[] = generateWrappedVertexArray(2, "tbs42", false, tg, NodeType.TBS);
        addRing(dxt4[1], tbs42, tg);

        return tg;
    }

    private static TopologyVertex[] generateWrappedVertexArray(int amount, String baseName, boolean isPoP, TestGraph tg, NodeType nodeType) {
        Map<NprNfvTypes.Type, Integer> deploymentCost = new HashMap<>();

        for (NprNfvTypes.Type type : NprNfvTypes.Type.values()) {
            deploymentCost.put(type, 40);
        }

        Map<NprNfvTypes.Type, Double> noHwAccel = new HashMap<>();
        for (NprNfvTypes.Type type : NprNfvTypes.Type.values()) {
            noHwAccel.put(type, 1.0);
        }

        Map<NprNfvTypes.Type, Double> hwAccel = new HashMap<>();
        for (NprNfvTypes.Type type : NprNfvTypes.Type.values()) {
            hwAccel.put(type, 1.0);
        }
        hwAccel.put(NprNfvTypes.Type.TRANSCODER, 0.25);

        Map<NprResources, Integer> resourceCapacity = new HashMap<>();
        resourceCapacity.put(NprResources.CPU_CORES, 8);
        resourceCapacity.put(NprResources.RAM_IN_GB, 16);


        TopologyVertex[] vertArr = new TopologyVertex[amount];
        for (int i = 0; i < vertArr.length; i++) {
            WrappedVertex wrapped = null;
            String name = baseName + (i + 1);
            DeviceId deviceIdOfName = devId(name);
            if (isPoP) {
                if (name.equals("dxtt2") || name.equals("dxt31")) { // TODO dirty af
                    System.out.println(name + " gets hw accel");
                    wrapped = new WrappedPoPVertex(new DefaultTopologyVertex(deviceIdOfName), deploymentCost, hwAccel, resourceCapacity);
                } else {
                    wrapped = new WrappedPoPVertex(new DefaultTopologyVertex(deviceIdOfName), deploymentCost, noHwAccel, resourceCapacity);
                }
            } else {
                wrapped = new WrappedVertex(new DefaultTopologyVertex(deviceIdOfName));
            }
            vertArr[i] = wrapped;
            tg.addVertex(wrapped, nodeType);
        }

        return vertArr;
    }

    private static void addStar(TopologyVertex dxt, TopologyVertex[] tbs, TestGraph tg) {
        for (TopologyVertex baseStation : tbs) {
            tg.addDirectionalEdgesBetween(baseStation, dxt);
        }
    }

    private static void addRing(TopologyVertex dxt, TopologyVertex[] tbs, TestGraph tg) {
        tg.addDirectionalEdgesBetween(dxt, tbs[0]);
        tg.addDirectionalEdgesBetween(dxt, tbs[tbs.length - 1]);
        for (int i = 0; i < tbs.length - 1; i++) {
            tg.addDirectionalEdgesBetween(tbs[i], tbs[i + 1]);
        }
    }


    private static DeviceId devId(String devIdString) {
        return DeviceId.deviceId(devIdString);
    }
}
