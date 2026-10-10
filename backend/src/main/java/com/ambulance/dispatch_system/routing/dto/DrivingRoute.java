package com.ambulance.dispatch_system.routing.dto;

import java.util.List;

/** Road geometry in GeoJSON coordinate order: longitude, latitude. */
public record DrivingRoute(
        List<List<Double>> coordinates,
        List<List<Double>> waypoints,
        double distanceKm,
        double durationMinutes) {}
