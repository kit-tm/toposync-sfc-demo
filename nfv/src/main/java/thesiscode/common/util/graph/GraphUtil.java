package thesiscode.common.util.graph;

import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphUtil {

    /**
     * Converts a set of edges to a map of verts which are mapped to a list of edges.
     * Each vertex is mapped to the edges whose source is this vertex.
     *
     * @param edges the edges
     * @return the map
     */
    public static Map<TopologyVertex, List<TopologyEdge>> getEdgesBySource(Set<TopologyEdge> edges) {
        Map<TopologyVertex, List<TopologyEdge>> edgesBySrc = new HashMap<>();
        edges.forEach(e -> {
            TopologyVertex src = e.src();
            edgesBySrc.computeIfAbsent(src, k -> new ArrayList<>());
            edgesBySrc.get(e.src()).add(e);
        });
        return edgesBySrc;
    }

}
