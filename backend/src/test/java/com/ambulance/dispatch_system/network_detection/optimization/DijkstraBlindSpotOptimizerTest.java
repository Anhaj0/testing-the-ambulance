package com.ambulance.dispatch_system.network_detection.optimization;

import com.ambulance.dispatch_system.common.entity.RoadEdge;
import com.ambulance.dispatch_system.common.entity.RoadNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DijkstraBlindSpotOptimizerTest {

    private DijkstraBlindSpotOptimizer optimizer;

    @BeforeEach
    void setUp() {
        optimizer = new DijkstraBlindSpotOptimizer();
    }

    @Test
    void testNodeReachableWithinThresholdNotBlindSpot() {
        RoadNode baseNode = createNode(1L, "Station Alpha");
        RoadNode nearbyNode = createNode(2L, "Nearby Town");
        RoadEdge edge = createEdge(1L, baseNode, nearbyNode, 5.0, false);

        List<RoadNode> blindSpots = optimizer.computeBlindSpots(
                List.of(baseNode, nearbyNode),
                List.of(edge),
                Set.of("Station Alpha"),
                10.0
        );

        assertTrue(blindSpots.isEmpty());
    }

    @Test
    void testNodeBeyondThresholdIdentifiedAsBlindSpot() {
        RoadNode baseNode = createNode(1L, "Station Alpha");
        RoadNode farNode = createNode(2L, "Remote Village");
        RoadEdge edge = createEdge(1L, baseNode, farNode, 20.0, false);

        List<RoadNode> blindSpots = optimizer.computeBlindSpots(
                List.of(baseNode, farNode),
                List.of(edge),
                Set.of("Station Alpha"),
                10.0
        );

        assertEquals(1, blindSpots.size());
        assertEquals("Remote Village", blindSpots.get(0).getName());
    }

    @Test
    void testBlockedEdgeIsExcluded() {
        RoadNode baseNode = createNode(1L, "Station Alpha");
        RoadNode isolatedNode = createNode(2L, "Blocked Town");
        RoadEdge blockedEdge = createEdge(1L, baseNode, isolatedNode, 5.0, true);

        List<RoadNode> blindSpots = optimizer.computeBlindSpots(
                List.of(baseNode, isolatedNode),
                List.of(blockedEdge),
                Set.of("Station Alpha"),
                10.0
        );

        assertEquals(1, blindSpots.size());
        assertEquals("Blocked Town", blindSpots.get(0).getName());
    }

    @Test
    void testFallbackWhenNoExplicitBaseNodesFound() {
        RoadNode nodeA = createNode(1L, "Node A");
        RoadNode nodeB = createNode(2L, "Node B");
        RoadEdge edge = createEdge(1L, nodeA, nodeB, 5.0, false);

        List<RoadNode> blindSpots = optimizer.computeBlindSpots(
                List.of(nodeA, nodeB),
                List.of(edge),
                Set.of(),
                10.0
        );

        assertTrue(blindSpots.isEmpty());
    }

    private RoadNode createNode(Long id, String name) {
        RoadNode node = new RoadNode();
        node.setId(id);
        node.setName(name);
        return node;
    }

    private RoadEdge createEdge(Long id, RoadNode from, RoadNode to, double timeMinutes, boolean blocked) {
        RoadEdge edge = new RoadEdge();
        edge.setId(id);
        edge.setFromNode(from);
        edge.setToNode(to);
        edge.setTravelTimeMinutes(timeMinutes);
        edge.setBlocked(blocked);
        return edge;
    }
}