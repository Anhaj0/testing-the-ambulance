package com.ambulance.dispatch_system.routing.algorithm;

import com.ambulance.dispatch_system.common.entity.RoadEdge;
import com.ambulance.dispatch_system.common.entity.RoadNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * All nodes share the same coordinates so the Haversine heuristic is always
 * zero - this isolates the tests from heuristic/geometry concerns and lets
 * edge weights alone determine the expected shortest path.
 */
class AStarAlgorithmTest {

    private final AStarAlgorithm aStar = new AStarAlgorithm();

    private RoadNode node(long id, String name) {
        RoadNode node = new RoadNode();
        node.setId(id);
        node.setName(name);
        node.setLatitude(0.0);
        node.setLongitude(0.0);
        return node;
    }

    private RoadEdge edge(RoadNode from, RoadNode to, double travelTimeMinutes) {
        RoadEdge edge = new RoadEdge();
        edge.setFromNode(from);
        edge.setToNode(to);
        edge.setDistanceKm(travelTimeMinutes);
        edge.setTravelTimeMinutes(travelTimeMinutes);
        return edge;
    }

    @Test
    void returnsStartOnlyWhenStartEqualsDestination() {
        RoadNode a = node(1, "A");

        List<RoadNode> path = aStar.findShortestPath(a, a, List.of());

        assertEquals(List.of(a), path);
    }

    @Test
    void findsTheOnlyPathThroughASimpleChain() {
        RoadNode a = node(1, "A");
        RoadNode b = node(2, "B");
        RoadNode c = node(3, "C");

        List<RoadEdge> edges = List.of(edge(a, b, 5), edge(b, c, 5));

        List<RoadNode> path = aStar.findShortestPath(a, c, edges);

        assertEquals(List.of(a, b, c), path);
    }

    @Test
    void returnsEmptyListWhenNoPathExists() {
        RoadNode a = node(1, "A");
        RoadNode b = node(2, "B");
        RoadNode isolated = node(3, "Isolated");

        List<RoadEdge> edges = List.of(edge(a, b, 5));

        assertTrue(aStar.findShortestPath(a, isolated, edges).isEmpty());
    }

    @Test
    void neverRoutesThroughABlockedEdge() {
        RoadNode a = node(1, "A");
        RoadNode b = node(2, "B");
        RoadNode c = node(3, "C");

        RoadEdge direct = edge(a, c, 1);
        direct.setBlocked(true);
        RoadEdge viaB1 = edge(a, b, 5);
        RoadEdge viaB2 = edge(b, c, 5);

        List<RoadNode> path = aStar.findShortestPath(a, c, List.of(direct, viaB1, viaB2));

        assertEquals(List.of(a, b, c), path);
    }

    /**
     * A -> B -> D is discovered first (cost 1 + 10 = 11) and enqueues D with that cost.
     * A -> C -> D (cost 1 + 1 = 2) is discovered afterwards and relaxes D to a cheaper
     * gScore, adding a second, better queue entry for D rather than mutating the first
     * one in place. The algorithm must follow the cheaper path, and must not be confused
     * by the stale, costlier entry for D still sitting in the queue behind it.
     */
    @Test
    void choosesTheCheaperPathWhenANodeIsRelaxedMultipleTimes() {
        RoadNode a = node(1, "A");
        RoadNode b = node(2, "B");
        RoadNode c = node(3, "C");
        RoadNode d = node(4, "D");

        List<RoadEdge> edges = List.of(
                edge(a, b, 1),
                edge(b, d, 10),
                edge(a, c, 1),
                edge(c, d, 1)
        );

        List<RoadNode> path = aStar.findShortestPath(a, d, edges);

        assertEquals(List.of(a, c, d), path);
    }

    /**
     * Every node lies on a roughly-equal-cost path into the shared destination E, so
     * each of B, C and D gets enqueued and later re-relaxed to a slightly better score
     * as cheaper alternates are found - forcing several stale entries to accumulate in
     * the open set before E is finally reached. Guards against the priority queue's
     * ordering silently breaking under repeated relaxation of many nodes at once.
     */
    @Test
    void findsShortestPathAcrossAWiderGraphWithManyRelaxations() {
        RoadNode a = node(1, "A");
        RoadNode b = node(2, "B");
        RoadNode c = node(3, "C");
        RoadNode d = node(4, "D");
        RoadNode e = node(5, "E");

        List<RoadEdge> edges = List.of(
                edge(a, b, 4),
                edge(a, c, 1),
                edge(c, b, 1),   // relaxes B down to 2 after it was first queued at 4
                edge(b, d, 4),
                edge(c, d, 2),   // relaxes D down to 3 after it was first queued at 6 (via B)
                edge(d, e, 1),
                edge(b, e, 10)
        );

        List<RoadNode> path = aStar.findShortestPath(a, e, edges);

        assertEquals(List.of(a, c, d, e), path);
    }
}
