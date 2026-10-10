package com.ambulance.dispatch_system.network_detection.dto;

public record RoadEdgeDto(
        Long id,
        String fromNode,
        String toNode,
        double distanceKm,
        double travelTimeMinutes,
        boolean blocked
) {
}
