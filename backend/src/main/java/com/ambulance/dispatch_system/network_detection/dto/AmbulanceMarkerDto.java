package com.ambulance.dispatch_system.network_detection.dto;

public record AmbulanceMarkerDto(
        Long id,
        String vehicleNumber,
        String currentLocationNode,
        String status,
        Double latitude,
        Double longitude
) {
}
