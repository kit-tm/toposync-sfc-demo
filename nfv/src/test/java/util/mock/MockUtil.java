package util.mock;

import org.mockito.Mockito;
import org.onosproject.net.topology.DefaultTopologyEdge;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;

public class MockUtil {
    public static TopologyEdge mockEdge(TopologyVertex src, TopologyVertex dst) {
        TopologyEdge edge = Mockito.mock(DefaultTopologyEdge.class, Mockito.withSettings().stubOnly());
        Mockito.when(edge.src()).thenReturn(src);
        Mockito.when(edge.dst()).thenReturn(dst);
        return edge;
    }
}
