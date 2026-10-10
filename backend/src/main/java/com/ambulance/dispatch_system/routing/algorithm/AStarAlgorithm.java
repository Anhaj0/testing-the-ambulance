package com.ambulance.dispatch_system.routing.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.ambulance.dispatch_system.common.entity.RoadEdge;
import com.ambulance.dispatch_system.common.entity.RoadNode;

public class AStarAlgorithm {

    public List<RoadNode> findShortestPath(
            RoadNode start,
            RoadNode destination,
            List<RoadEdge> edges) {

        Map<Long, List<RoadEdge>> graph = buildGraph(edges);

        Map<Long, Double> gScore = new HashMap<>();
        Map<Long, RoadNode> previous = new HashMap<>();

        // Each queue entry snapshots the gScore/fScore it was inserted with.
        // java.util.PriorityQueue fixes an element's position in the heap
        // using compare() only at insertion time and never re-sifts it if
        // the compared value changes afterwards - so relaxing a node that is
        // already queued must add a fresh entry rather than mutate the old
        // one's priority in place (that would silently corrupt the heap's
        // ordering invariant and could make poll() return a non-optimal
        // node). The stale entry left behind is skipped when polled, the
        // same lazy-deletion pattern DijkstraBlindSpotOptimizer already uses.
        PriorityQueue<NodeEntry> openSet =
                new PriorityQueue<>(Comparator.comparingDouble(NodeEntry::fScore));

        gScore.put(start.getId(), 0.0);

        openSet.add(new NodeEntry(start, 0.0, heuristic(start, destination)));

        while (!openSet.isEmpty()) {

            NodeEntry currentEntry = openSet.poll();
            RoadNode current = currentEntry.node();

            // A cheaper relaxation superseded this entry after it was queued; skip it.
            if (currentEntry.gScore() > gScore.getOrDefault(current.getId(), Double.MAX_VALUE)) {
                continue;
            }

            if (current.getId().equals(destination.getId())) {
                return reconstructPath(previous, current);
            }

            for (RoadEdge edge : graph.getOrDefault(
                    current.getId(),
                    Collections.emptyList())) {

                RoadNode neighbour = edge.getToNode();

                double tentativeGScore =
                        currentEntry.gScore()
                                + edge.getTravelTimeMinutes();

                if (tentativeGScore <
                        gScore.getOrDefault(
                                neighbour.getId(),
                                Double.MAX_VALUE)) {

                    previous.put(
                            neighbour.getId(),
                            current);

                    gScore.put(
                            neighbour.getId(),
                            tentativeGScore);

                    // f(n) = g(n) + h(n)
                    double neighbourFScore =
                            tentativeGScore
                                    + heuristic(
                                            neighbour,
                                            destination);

                    openSet.add(new NodeEntry(neighbour, tentativeGScore, neighbourFScore));
                }
            }
        }

        return Collections.emptyList();
    }

    /** A node queued with the gScore/fScore it had at insertion time, so the heap's priority never mutates in place. */
    private record NodeEntry(RoadNode node, double gScore, double fScore) {}

    private Map<Long, List<RoadEdge>> buildGraph(
            List<RoadEdge> edges) {

        Map<Long, List<RoadEdge>> graph =
                new HashMap<>();

        for (RoadEdge edge : edges) {

            if (edge.isBlocked()) {
                continue;
            }

            graph.computeIfAbsent(
                    edge.getFromNode().getId(),
                    key -> new ArrayList<>())
                    .add(edge);
        }

        return graph;
    }

    /*
     * Heuristic:
     * Estimates the remaining travel time (minutes)
     * using the Haversine distance formula.
     */
    private double heuristic(
            RoadNode current,
            RoadNode destination) {

        double earthRadiusKm = 6371.0;

        double lat1 = Math.toRadians(current.getLatitude());
        double lat2 = Math.toRadians(destination.getLatitude());

        double latitudeDifference =
                Math.toRadians(
                        destination.getLatitude()
                                - current.getLatitude());

        double longitudeDifference =
                Math.toRadians(
                        destination.getLongitude()
                                - current.getLongitude());

        double a =
                Math.sin(latitudeDifference / 2)
                        * Math.sin(latitudeDifference / 2)
                        + Math.cos(lat1)
                        * Math.cos(lat2)
                        * Math.sin(longitudeDifference / 2)
                        * Math.sin(longitudeDifference / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a));

        double distanceKm = earthRadiusKm * c;

        // Assume ambulance average speed = 40 km/h
        double averageSpeedKmPerHour = 40.0;

        return (distanceKm / averageSpeedKmPerHour) * 60.0;
    }

    private List<RoadNode> reconstructPath(
            Map<Long, RoadNode> previous,
            RoadNode current) {

        LinkedList<RoadNode> path =
                new LinkedList<>();

        while (current != null) {

            path.addFirst(current);

            current = previous.get(current.getId());
        }

        return path;
    }
}