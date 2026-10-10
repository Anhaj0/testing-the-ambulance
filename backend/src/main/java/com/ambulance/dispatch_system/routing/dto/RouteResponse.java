package com.ambulance.dispatch_system.routing.dto;

import java.util.List;

import com.ambulance.dispatch_system.common.entity.RoadNode;

public class RouteResponse {

    private String algorithm;
    private double totalTravelTimeMinutes;
    private double totalDistanceKm;
    private List<RoadNode> route;
    private DrivingRoute drivingRoute;

    public RouteResponse() {
    }

    public RouteResponse(
            String algorithm,
            double totalTravelTimeMinutes,
            double totalDistanceKm,
            List<RoadNode> route) {

        this.algorithm = algorithm;
        this.totalTravelTimeMinutes = totalTravelTimeMinutes;
        this.totalDistanceKm = totalDistanceKm;
        this.route = route;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public double getTotalTravelTimeMinutes() {
        return totalTravelTimeMinutes;
    }

    public void setTotalTravelTimeMinutes(double totalTravelTimeMinutes) {
        this.totalTravelTimeMinutes = totalTravelTimeMinutes;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public List<RoadNode> getRoute() {
        return route;
    }

    public void setRoute(List<RoadNode> route) {
        this.route = route;
    }

    public DrivingRoute getDrivingRoute() {
        return drivingRoute;
    }

    public void setDrivingRoute(DrivingRoute drivingRoute) {
        this.drivingRoute = drivingRoute;
    }
}
