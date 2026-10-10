package com.ambulance.dispatch_system.network_detection.optimization;

import com.ambulance.dispatch_system.common.entity.RoadEdge;
import com.ambulance.dispatch_system.common.entity.RoadNode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DijkstraBlindSpotOptimizer {

    public List<RoadNode> computeBlindSpots(List<RoadNode> allNodes,
                                            List<RoadEdge> allEdges,
                                            Set<String> baseNodeNames,
                                            double thresholdMinutes) {
        if (allNodes == null || allNodes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Double> minTravelTimes = computeMinTravelTimes(allNodes, allEdges, baseNodeNames);

        List<RoadNode> blindSpots = new ArrayList<>();
        for (RoadNode node : allNodes) {
            if (node == null || node.getId() == null) continue;
            Double time = minTravelTimes.get(node.getId());
            if (time == null || time > thresholdMinutes) {
                blindSpots.add(node);
            }
        }

        return blindSpots;
    }

    /**
     * The threshold-independent half of blind-spot detection: one multi-source Dijkstra pass
     * giving the minimum travel time from the nearest base node to every reachable node.
     * Extracted so a caller that needs to check several thresholds (see
     * NetworkCoverageStatsService) can run the traversal once and filter the result repeatedly,
     * instead of paying for a fresh pass - and a fresh database fetch of the whole road network -
     * per threshold.
     */
    public Map<Long, Double> computeMinTravelTimes(List<RoadNode> allNodes,
                                                     List<RoadEdge> allEdges,
                                                     Set<String> baseNodeNames) {
        if (allNodes == null || allNodes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<RoadEdge>> adjList = new HashMap<>();
        if (allEdges != null) {
            for (RoadEdge edge : allEdges) {
                if (edge != null && !edge.isBlocked() && edge.getFromNode() != null && edge.getFromNode().getId() != null) {
                    adjList.computeIfAbsent(edge.getFromNode().getId(), k -> new ArrayList<>()).add(edge);
                }
            }
        }

        Map<Long, Double> minTravelTimes = new HashMap<>();
        for (RoadNode node : allNodes) {
            if (node != null && node.getId() != null) {
                minTravelTimes.put(node.getId(), Double.MAX_VALUE);
            }
        }

        PriorityQueue<NodeDistancePair> pq = new PriorityQueue<>(Comparator.comparingDouble(p -> p.distance));

        boolean foundExplicitBase = false;
        for (RoadNode node : allNodes) {
            if (node == null || node.getId() == null) continue;
            boolean isBase = baseNodeNames != null && baseNodeNames.contains(node.getName());

            if (isBase) {
                foundExplicitBase = true;
                minTravelTimes.put(node.getId(), 0.0);
                pq.add(new NodeDistancePair(node.getId(), 0.0));
            }
        }

        if (!foundExplicitBase && !allNodes.isEmpty() && allNodes.get(0) != null && allNodes.get(0).getId() != null) {
            RoadNode fallback = allNodes.get(0);
            minTravelTimes.put(fallback.getId(), 0.0);
            pq.add(new NodeDistancePair(fallback.getId(), 0.0));
        }

        while (!pq.isEmpty()) {
            NodeDistancePair curr = pq.poll();
            Double currentTime = minTravelTimes.get(curr.nodeId);
            if (currentTime == null || curr.distance > currentTime) continue;

            for (RoadEdge edge : adjList.getOrDefault(curr.nodeId, Collections.emptyList())) {
                double newDist = curr.distance + edge.getTravelTimeMinutes();
                if (edge.getToNode() != null && edge.getToNode().getId() != null) {
                    Long neighborId = edge.getToNode().getId();
                    if (newDist < minTravelTimes.getOrDefault(neighborId, Double.MAX_VALUE)) {
                        minTravelTimes.put(neighborId, newDist);
                        pq.add(new NodeDistancePair(neighborId, newDist));
                    }
                }
            }
        }

        return minTravelTimes;
    }


    private record NodeDistancePair(Long nodeId, double distance) {}
}