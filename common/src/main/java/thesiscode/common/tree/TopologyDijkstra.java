package thesiscode.common.tree;

import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TopologyDijkstra extends AbstractTopologyPerSourceTreeAlgorithm {
    private Logger log = LoggerFactory.getLogger(getClass());

    private Set<TopologyVertex> vertices;
    private Map<TopologyVertex, Set<TopologyEdge>> edgesFrom;


    private TopologyVertex source;
    private Set<TopologyVertex> destinations;

    // dijkstra data structures
    private Map<TopologyVertex, Integer> distanceMap = new HashMap<>();
    private Map<TopologyVertex, TopologyVertex> predMap = new HashMap<>();
    private Set<TopologyVertex> toVisit = new HashSet<>();
    private Set<TopologyVertex> visited = new HashSet<>();

    public TopologyDijkstra(Set<TopologyVertex> vertices, Set<TopologyEdge> edges) {
        this.vertices = vertices;

        this.edgesFrom = new HashMap<>();
        for (TopologyEdge edge : edges) {
            edgesFrom.computeIfAbsent(edge.src(), k -> new HashSet<>());
            edgesFrom.get(edge.src()).add(edge);
        }
    }

    public Set<TopologyEdge> computeTree(DeviceId sourceId, Set<DeviceId> destinationIds) {
        init(sourceId, destinationIds);

        dijkstra();

        return getTreeEdges();
    }

    private void init(DeviceId sourceId, Set<DeviceId> destinationIds) {
        // Ids to vertexes
        Set<TopologyVertex> dsts = new HashSet<>();
        for (TopologyVertex vert : vertices) {
            if (vert.deviceId().equals(sourceId)) {
                this.source = vert;
            }
            if (destinationIds.contains(vert.deviceId())) {
                dsts.add(vert);
            }
        }
        this.destinations = dsts;

        distanceMap = new HashMap<>();
        predMap = new HashMap<>();
        toVisit = new HashSet<>();
        visited = new HashSet<>();

        // distance map
        for (TopologyVertex vert : vertices) {
            if (vert.deviceId().equals(source.deviceId())) {
                distanceMap.put(vert, 0);
            } else {
                distanceMap.put(vert, Integer.MAX_VALUE);
            }
        }

        // pred map
        predMap.put(source, source);

        // to visit
        toVisit.add(source);
    }

    /**
     * the actual dijkstra algorithm
     */
    private void dijkstra() {
        while (toVisit.size() > 0) {
            TopologyVertex cur = getNextVertex();
            log.debug("Visiting {}", cur);
            toVisit.remove(cur);
            for (TopologyEdge edge : edgesFrom.get(cur)) {
                TopologyVertex adjacentVert = edge.dst();
                if (!visited.contains(adjacentVert)) {
                    if (distanceMap.get(cur) + 1 < distanceMap.get(adjacentVert)) {
                        predMap.put(adjacentVert, cur);
                        distanceMap.put(adjacentVert, distanceMap.get(cur) + 1);
                    }
                    toVisit.add(adjacentVert);
                }
            }
            visited.add(cur);
        }
    }

    /**
     * calculates the next vertex to choose in the dijkstra algorithm, i.e. the vertex with the shortest distance
     *
     * @return the next vertex
     */
    private TopologyVertex getNextVertex() {
        Integer minDist = Integer.MAX_VALUE;
        TopologyVertex vertexWithMinDist = null;
        for (TopologyVertex vert : toVisit) {
            log.debug("getSwitchWithShortestDistance(): looking at {}", vert);
            Integer curDist = distanceMap.get(vert);
            if (curDist < minDist || curDist == Integer.MAX_VALUE) {
                log.debug("getSwitchWithShortestDistance(): new minimums: dpid={}, minDist={}", vert, curDist);
                minDist = curDist;
                vertexWithMinDist = vert;
            }
        }
        return vertexWithMinDist;
    }

    /**
     * traverses the tree from each receiver to the sender and adds all links on these paths to the tree
     *
     * @return the tree links
     */
    private Set<TopologyEdge> getTreeEdges() {
        Set<TopologyEdge> treeEdges = new HashSet<>();
        for (TopologyVertex receiver : destinations) {
            TopologyVertex nearReceiver = receiver;
            TopologyVertex oneHopBehind = predMap.get(receiver);
            log.debug("predmap: {}", predMap);

            while (nearReceiver != null && oneHopBehind != null && (!oneHopBehind.equals(nearReceiver))) {
                log.debug("adding edge from {} to {}", oneHopBehind, nearReceiver);
                TopologyEdge edge = null;
                try {
                    edge = getEdgeBetween(oneHopBehind, nearReceiver);
                } catch (LinkNotFoundException e) {
                    log.warn(e.getMessage());
                }
                treeEdges.add(edge);
                log.debug("added edge: {}", edge);
                nearReceiver = predMap.get(nearReceiver);
                oneHopBehind = predMap.get(oneHopBehind);
            }
        }
        return treeEdges;
    }

    /**
     * returns the link from one vertex to another, if any
     *
     * @param from the source vertex
     * @param to   the destination vertex
     * @return the link
     * @throws LinkNotFoundException thrown if no link could be found
     */
    private TopologyEdge getEdgeBetween(TopologyVertex from, TopologyVertex to) throws LinkNotFoundException {
        Set<TopologyEdge> outEdgesFrom = edgesFrom.get(from);
        for (TopologyEdge edge : outEdgesFrom) {
            if (edge.dst().equals(to)) {
                return edge;
            }
        }
        throw new LinkNotFoundException(String.format("Link from %s to %s was not found.", from, to));
    }

}
