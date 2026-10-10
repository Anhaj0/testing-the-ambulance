package com.ambulance.dispatch_system.network_detection.service;

import com.ambulance.dispatch_system.common.entity.RoadNode;

import java.util.List;
import java.util.Map;

/**
 * The result of one multi-source Dijkstra pass: every node in the network, alongside the
 * minimum travel time from the nearest available ambulance base to reach it. Threshold-
 * independent, so it can be filtered or counted against several thresholds without re-running
 * the traversal - see NetworkCoverageStatsService, which builds a whole coverage-vs-threshold
 * curve from a single instance instead of one Dijkstra pass (and one database fetch of the
 * whole road network) per threshold.
 *
 * @see NetworkAnalysisService#computeReachability()
 */
public record NetworkReachability(List<RoadNode> allNodes, Map<Long, Double> minTravelTimesByNodeId) {

    public int totalNodeCount() {
        return allNodes.size();
    }

    /** Nodes with no known route to a base, or whose best route exceeds thresholdMinutes. */
    public List<RoadNode> blindSpots(double thresholdMinutes) {
        return allNodes.stream()
                .filter(node -> node != null && node.getId() != null)
                .filter(node -> isBlindSpot(node, thresholdMinutes))
                .toList();
    }

    public int blindSpotCount(double thresholdMinutes) {
        int count = 0;
        for (RoadNode node : allNodes) {
            if (node != null && node.getId() != null && isBlindSpot(node, thresholdMinutes)) {
                count++;
            }
        }
        return count;
    }

    private boolean isBlindSpot(RoadNode node, double thresholdMinutes) {
        Double time = minTravelTimesByNodeId.get(node.getId());
        return time == null || time > thresholdMinutes;
    }
}
