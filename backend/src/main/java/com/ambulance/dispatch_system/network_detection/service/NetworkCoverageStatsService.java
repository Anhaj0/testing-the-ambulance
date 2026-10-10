package com.ambulance.dispatch_system.network_detection.service;

import com.ambulance.dispatch_system.network_detection.dto.CoverageStatsDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a coverage-vs-threshold curve by reusing the teammate's existing blind-spot analysis
 * (NetworkAnalysisService) via composition rather than reimplementing it. A single
 * NetworkReachability (one Dijkstra pass over the whole road network) is computed once and then
 * filtered against every threshold, rather than calling findBlindSpots once per threshold -
 * which would re-fetch the whole road network from the database and re-run the traversal from
 * scratch for each one.
 */
@Service
public class NetworkCoverageStatsService {

    private final NetworkAnalysisService analysisService;

    public NetworkCoverageStatsService(NetworkAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    public List<CoverageStatsDto> getCoverageCurve(List<Double> thresholds) {
        NetworkReachability reachability = analysisService.computeReachability();
        int totalNodes = reachability.totalNodeCount();

        return thresholds.stream()
                .map(threshold -> {
                    int blindCount = reachability.blindSpotCount(threshold);
                    double coveragePct = totalNodes > 0
                            ? ((totalNodes - blindCount) / (double) totalNodes) * 100.0
                            : 0.0;
                    return new CoverageStatsDto(threshold, totalNodes, blindCount, coveragePct);
                })
                .collect(Collectors.toList());
    }
}