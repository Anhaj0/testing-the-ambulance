package com.ambulance.dispatch_system.network_detection.controller;

import com.ambulance.dispatch_system.network_detection.dto.AmbulanceMarkerDto;
import com.ambulance.dispatch_system.network_detection.dto.RoadEdgeDto;
import com.ambulance.dispatch_system.network_detection.dto.RoadNodeDto;
import com.ambulance.dispatch_system.network_detection.service.NetworkGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/network/graph")
public class NetworkGraphController {

    private final NetworkGraphService networkGraphService;

    public NetworkGraphController(NetworkGraphService networkGraphService) {
        this.networkGraphService = networkGraphService;
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<RoadNodeDto>> getNodes() {
        return ResponseEntity.ok(networkGraphService.getAllNodes());
    }

    @GetMapping("/edges")
    public ResponseEntity<List<RoadEdgeDto>> getEdges() {
        return ResponseEntity.ok(networkGraphService.getAllEdges());
    }

    @GetMapping("/ambulances")
    public ResponseEntity<List<AmbulanceMarkerDto>> getAmbulances() {
        return ResponseEntity.ok(networkGraphService.getAllAmbulanceMarkers());
    }
}