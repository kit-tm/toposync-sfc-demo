package util.mock;

import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyVertex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestGraph implements TopologyGraph {
    private Set<TopologyVertex> vertices;
    private Set<TopologyVertex> tbs = new HashSet<>();
    private Set<TopologyVertex> dxt = new HashSet<>();
    private Set<TopologyVertex> dxtt = new HashSet<>();
    private Set<TopologyEdge> edges;
    private Map<TopologyVertex, Set<TopologyEdge>> in = new HashMap<>();
    private Map<TopologyVertex, Set<TopologyEdge>> out = new HashMap<>();

    public TestGraph(Set<TopologyVertex> vertices, Set<TopologyEdge> edges) {
        this.vertices = vertices;
        this.edges = edges;
        for (TopologyEdge edge : edges) {
            TopologyVertex fromVertex = edge.src();
            Set<TopologyEdge> outSet = out.get(fromVertex);
            if (outSet == null) {
                outSet = new HashSet<>();
                outSet.add(edge);
                out.put(fromVertex, outSet);
            } else {
                outSet.add(edge);
            }

            TopologyVertex toVertex = edge.dst();
            Set<TopologyEdge> inSet = in.get(toVertex);
            if (inSet == null) {
                inSet = new HashSet<>();
                inSet.add(edge);
                in.put(toVertex, inSet);
            } else {
                inSet.add(edge);
            }
        }

    }

    public void addVertex(TopologyVertex vert, NodeType nodeType) {
        vertices.add(vert);
        switch (nodeType) {
            case TBS:
                tbs.add(vert);
                break;
            case DXT:
                dxt.add(vert);
                break;
            case DXTT:
                dxtt.add(vert);
                break;
        }
    }

    public void addEdge(TopologyEdge edge) {
        if (!(vertices.contains(edge.src()) && vertices.contains(edge.dst()))) {
            throw new IllegalArgumentException(
                    "Both edge endpoints must be vertices of this graph in order to add " + "the edge.");
        }

        edges.add(edge);

        Set<TopologyEdge> fromEdgeSrc = out.get(edge.src());
        if (fromEdgeSrc == null) {
            fromEdgeSrc = new HashSet<>();
            fromEdgeSrc.add(edge);
            out.put(edge.src(), fromEdgeSrc);
        } else {
            fromEdgeSrc.add(edge);
        }

        Set<TopologyEdge> toEdgeDst = in.get(edge.dst());
        if (toEdgeDst == null) {

            toEdgeDst = new HashSet<>();
            toEdgeDst.add(edge);
            in.put(edge.dst(), toEdgeDst);
        } else {
            toEdgeDst.add(edge);
        }


    }

    @Override
    public Set<TopologyVertex> getVertexes() {
        return vertices;
    }

    public Set<TopologyVertex> getTbs() {
        return tbs;
    }

    public Set<TopologyVertex> getDxt() {
        return dxt;
    }

    public Set<TopologyVertex> getDxtt() {
        return dxtt;
    }

    @Override
    public Set<TopologyEdge> getEdges() {
        return edges;
    }

    @Override
    public Set<TopologyEdge> getEdgesFrom(TopologyVertex topologyVertex) {
        return out.get(topologyVertex);
    }

    @Override
    public Set<TopologyEdge> getEdgesTo(TopologyVertex topologyVertex) {
        return in.get(topologyVertex);
    }

    public TopologyVertex getById(DeviceId id) {
        for (TopologyVertex vertex : vertices) {
            if (vertex.deviceId().equals(id)) {
                return vertex;
            }
        }
        return null;
    }

    public void addDirectionalEdgesBetween(TopologyVertex src, TopologyVertex dst) {
        addEdge(MockUtil.mockEdge(src, dst));
        addEdge(MockUtil.mockEdge(dst, src));
    }

}
