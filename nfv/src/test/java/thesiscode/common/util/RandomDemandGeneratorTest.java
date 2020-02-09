package thesiscode.common.util;

import org.junit.Assert;
import org.junit.Test;
import org.onosproject.net.topology.TopologyVertex;
import thesiscode.common.topo.ConstantLinkWeigher;
import thesiscode.common.nfv.traffic.NprTraffic;
import util.mock.TopoRef20MockBuilder;

import java.util.List;
import java.util.Set;

public class RandomDemandGeneratorTest {
    private static final int EDGE_BANDWIDTH = 10;
    private static final int EDGE_DELAY = 1;

    @Test
    public void generateDemandTest() {
        for (int i = 0; i < 10000; i++) {
            RandomDemandGenerator generator = new RandomDemandGenerator(TopoRef20MockBuilder.getMockedTopo(), new ConstantLinkWeigher(EDGE_BANDWIDTH, EDGE_DELAY));
            List<NprTraffic> generatedTraffic = generator.generateDemand();
            int demandSum = 0;
            for (NprTraffic traffic : generatedTraffic) {
                TopologyVertex ingress = traffic.getIngressNode();
                Assert.assertNotNull(ingress);
                Set<TopologyVertex> egress = traffic.getEgressNodes();
                Assert.assertNotNull(egress);
                Assert.assertTrue("egress does not contain nodes", egress.size() >= 1);
                Assert.assertFalse("ingress is contained in egress", egress.contains(ingress));
                demandSum += traffic.getDemand();
            }
            Assert.assertTrue("overall demand sum is larger than edge bandwidth", demandSum <= EDGE_BANDWIDTH);
        }
    }

    private void ingressAndEgressOverlapTest(TopologyVertex ingress, Set<TopologyVertex> egress) {
        Assert.assertFalse(egress.contains(ingress));
    }

}
