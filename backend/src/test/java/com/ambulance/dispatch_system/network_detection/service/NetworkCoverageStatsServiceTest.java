package com.ambulance.dispatch_system.network_detection.service;

import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.network_detection.dto.CoverageStatsDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NetworkCoverageStatsServiceTest {

    private static final double DELTA = 1e-9;

    private RoadNode node(long id, String name) {
        RoadNode n = new RoadNode();
        n.setId(id);
        n.setName(name);
        return n;
    }

    @Test
    void computesTheWholeCurveFromASingleReachabilityPass() {
        NetworkAnalysisService analysisService = Mockito.mock(NetworkAnalysisService.class);

        RoadNode near = node(1, "Near");
        RoadNode mid = node(2, "Mid");
        RoadNode far = node(3, "Far");
        NetworkReachability reachability = new NetworkReachability(
                List.of(near, mid, far), Map.of(1L, 5.0, 2L, 15.0, 3L, 25.0));
        when(analysisService.computeReachability()).thenReturn(reachability);

        NetworkCoverageStatsService coverageStatsService = new NetworkCoverageStatsService(analysisService);

        List<CoverageStatsDto> curve = coverageStatsService.getCoverageCurve(List.of(10.0, 20.0, 30.0));

        // computeReachability (the database fetch + Dijkstra pass) must run exactly once for the
        // whole curve, not once per threshold - that's the fix this test guards against.
        verify(analysisService, times(1)).computeReachability();

        assertEquals(3, curve.size());
        assertCoverage(curve.get(0), 10.0, 3, 2);
        assertCoverage(curve.get(1), 20.0, 3, 1);
        assertCoverage(curve.get(2), 30.0, 3, 0);
    }

    @Test
    void reportsZeroCoverageWhenThereAreNoNodes() {
        NetworkAnalysisService analysisService = Mockito.mock(NetworkAnalysisService.class);
        when(analysisService.computeReachability()).thenReturn(new NetworkReachability(List.of(), Map.of()));

        NetworkCoverageStatsService coverageStatsService = new NetworkCoverageStatsService(analysisService);

        List<CoverageStatsDto> curve = coverageStatsService.getCoverageCurve(List.of(10.0));

        assertCoverage(curve.get(0), 10.0, 0, 0);
        assertEquals(0.0, curve.get(0).coveragePercentage(), DELTA);
    }

    /** Compares against the same (totalNodes - blindCount) / totalNodes * 100.0 formula the production code uses. */
    private void assertCoverage(CoverageStatsDto actual, double threshold, int totalNodes, int blindCount) {
        assertEquals(threshold, actual.thresholdMinutes(), DELTA);
        assertEquals(totalNodes, actual.totalNodes());
        assertEquals(blindCount, actual.blindSpotCount());
        if (totalNodes > 0) {
            double expectedPct = ((totalNodes - blindCount) / (double) totalNodes) * 100.0;
            assertEquals(expectedPct, actual.coveragePercentage(), DELTA);
        }
    }
}
