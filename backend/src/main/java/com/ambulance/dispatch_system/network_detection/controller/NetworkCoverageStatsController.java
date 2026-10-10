package com.ambulance.dispatch_system.network_detection.controller;

import com.ambulance.dispatch_system.network_detection.dto.CoverageStatsDto;
import com.ambulance.dispatch_system.network_detection.service.NetworkCoverageStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/network/coverage")
public class NetworkCoverageStatsController {

    private final NetworkCoverageStatsService coverageStatsService;

    public NetworkCoverageStatsController(NetworkCoverageStatsService coverageStatsService) {
        this.coverageStatsService = coverageStatsService;
    }

    // e.g. GET /api/v1/network/coverage?thresholds=5,10,15,20,25,30
    @GetMapping
    public ResponseEntity<List<CoverageStatsDto>> getCoverageCurve(
            @RequestParam(defaultValue = "5,10,15,20,25,30,35,40") String thresholds) {

        List<Double> parsed = Arrays.stream(thresholds.split(","))
                .map(String::trim)
                .map(Double::parseDouble)
                .collect(Collectors.toList());

        return ResponseEntity.ok(coverageStatsService.getCoverageCurve(parsed));
    }
}