package com.ambulance.dispatch_system.routing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ambulance.dispatch_system.routing.dto.RouteRequest;
import com.ambulance.dispatch_system.routing.dto.RouteResponse;
import com.ambulance.dispatch_system.routing.service.RouteService;
import com.ambulance.dispatch_system.routing.service.DrivingRouteService;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteService routeService;
    private final DrivingRouteService drivingRouteService;

    public RouteController(RouteService routeService, DrivingRouteService drivingRouteService) {
        this.routeService = routeService;
        this.drivingRouteService = drivingRouteService;
    }

    @PostMapping
    public ResponseEntity<RouteResponse> findRoute(
            @RequestBody RouteRequest request) {

        RouteResponse response = routeService.findRoute(request);
        response.setDrivingRoute(drivingRouteService.findDrivingRoute(response.getRoute()));

        return ResponseEntity.ok(response);
    }
}
