package com.ambulance.dispatch_system.network_detection.controller;

import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.network_detection.service.NetworkAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/network")
public class NetworkAnalysisController {

    private final NetworkAnalysisService networkAnalysisService;

    public NetworkAnalysisController(NetworkAnalysisService networkAnalysisService) {
        this.networkAnalysisService = networkAnalysisService;
    }

    @GetMapping("/blind-spots")
    public ResponseEntity<List<RoadNode>> getBlindSpots(@RequestParam(defaultValue = "10.0") double thresholdMinutes) {
        return ResponseEntity.ok(networkAnalysisService.findBlindSpots(thresholdMinutes));
    }
}