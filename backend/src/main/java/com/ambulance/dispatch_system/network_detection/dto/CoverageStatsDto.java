package com.ambulance.dispatch_system.network_detection.dto;

public record CoverageStatsDto(
        double thresholdMinutes,
        int totalNodes,
        int blindSpotCount,
        double coveragePercentage
) {
}
