package com.ambulance.dispatch_system.network_detection.service;

import com.ambulance.dispatch_system.common.entity.RoadNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkReachabilityTest {

    private RoadNode node(long id, String name) {
        RoadNode n = new RoadNode();
        n.setId(id);
        n.setName(name);
        return n;
    }

    @Test
    void countsAndListsNodesBeyondTheThreshold() {
        RoadNode near = node(1, "Near");
        RoadNode far = node(2, "Far");
        RoadNode unreachable = node(3, "Unreachable"); // no entry at all in the travel-time map

        NetworkReachability reachability = new NetworkReachability(
                List.of(near, far, unreachable),
                Map.of(1L, 5.0, 2L, 15.0));

        assertEquals(3, reachability.totalNodeCount());
        assertEquals(2, reachability.blindSpotCount(10.0));
        assertEquals(List.of(far, unreachable), reachability.blindSpots(10.0));
    }

    @Test
    void aLowerThresholdNeverProducesFewerBlindSpotsThanAHigherOne() {
        RoadNode a = node(1, "A");
        RoadNode b = node(2, "B");
        NetworkReachability reachability = new NetworkReachability(
                List.of(a, b), Map.of(1L, 8.0, 2L, 12.0));

        assertTrue(reachability.blindSpotCount(5.0) >= reachability.blindSpotCount(20.0));
    }

    @Test
    void sameReachabilityAnsweredDifferentlyAtDifferentThresholds() {
        // The whole point of NetworkReachability: one Dijkstra result, filtered several ways -
        // no re-computation needed to answer a different threshold.
        RoadNode a = node(1, "A");
        NetworkReachability reachability = new NetworkReachability(List.of(a), Map.of(1L, 10.0));

        assertEquals(1, reachability.blindSpotCount(5.0));
        assertEquals(0, reachability.blindSpotCount(15.0));
    }
}
