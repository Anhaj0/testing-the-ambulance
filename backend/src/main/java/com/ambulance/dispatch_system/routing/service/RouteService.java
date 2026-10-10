package com.ambulance.dispatch_system.routing.service;

import com.ambulance.dispatch_system.routing.dto.RouteRequest;
import com.ambulance.dispatch_system.routing.dto.RouteResponse;

public interface RouteService {

    /**
     * Finds the best available route using the selected routing algorithm.
     */
    RouteResponse findRoute(RouteRequest request);

    /**
     * Finds the best available route between two locations.
     * This method is exposed so other modules can reuse
     * Task 1's routing functionality.
     */
    RouteResponse findRoute(
            Long startLocationId,
            Long destinationLocationId);

    /**
     * Finds the best available route between two locations by their node names.
     */
    RouteResponse findRoute(
            String startNodeName,
            String destinationNodeName);

    /**
     * Loads the current unblocked road network once, for a caller that needs
     * to run several route queries against the same graph (e.g. scoring
     * every candidate ambulance for one dispatch decision) without
     * re-fetching the full edge list from the database on every single query.
     */
    RoutingSnapshot loadSnapshot();
}