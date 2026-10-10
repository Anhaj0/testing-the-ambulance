package com.ambulance.dispatch_system.routing.service;

import com.ambulance.dispatch_system.routing.dto.RouteResponse;

/**
 * A road network graph loaded once and reused for repeated route queries,
 * instead of paying a fresh database round trip for the full edge list on
 * every {@link RouteService#findRoute(String, String)} call.
 *
 * <p>Obtain one via {@link RouteService#loadSnapshot()}. Intended for a
 * caller that needs to score many candidates against the same graph in one
 * operation - e.g. every available ambulance for a single dispatch decision -
 * where re-fetching the full edge list per candidate would mean N database
 * round trips for identical, unchanged data.
 */
@FunctionalInterface
public interface RoutingSnapshot {

    RouteResponse findRoute(String startNodeName, String destinationNodeName);
}
