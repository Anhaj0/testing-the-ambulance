package com.ambulance.dispatch_system.routing.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ambulance.dispatch_system.common.entity.RoadEdge;
import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.common.repository.RoadEdgeRepository;
import com.ambulance.dispatch_system.common.repository.RoadNodeRepository;
import com.ambulance.dispatch_system.routing.algorithm.AStarAlgorithm;
import com.ambulance.dispatch_system.routing.dto.RouteRequest;
import com.ambulance.dispatch_system.routing.dto.RouteResponse;
import com.ambulance.dispatch_system.routing.exception.LocationNotFoundException;
import com.ambulance.dispatch_system.routing.exception.RouteNotFoundException;

@Service
public class RouteServiceImpl implements RouteService {

    private final RoadNodeRepository roadNodeRepository;
    private final RoadEdgeRepository roadEdgeRepository;
    private final AStarAlgorithm aStarAlgorithm;

    public RouteServiceImpl(
            RoadNodeRepository roadNodeRepository,
            RoadEdgeRepository roadEdgeRepository) {

        this.roadNodeRepository = roadNodeRepository;
        this.roadEdgeRepository = roadEdgeRepository;
        this.aStarAlgorithm = new AStarAlgorithm();
    }

    @Override
    public RouteResponse findRoute(RouteRequest request) {

        return findRoute(
                request.getStartLocationId(),
                request.getDestinationLocationId()
        );
    }

    /**
     * Reusable routing method for other modules.
     *
     * Other modules can use Task 1's A* routing
     * without implementing their own routing algorithm.
     */
    @Override
    public RouteResponse findRoute(
            Long startLocationId,
            Long destinationLocationId) {

        RoadNode start = roadNodeRepository
                .findById(startLocationId)
                .orElseThrow(() ->
                        new LocationNotFoundException(
                                "Start location not found"));

        RoadNode destination = roadNodeRepository
                .findById(destinationLocationId)
                .orElseThrow(() ->
                        new LocationNotFoundException(
                                "Destination location not found"));

        List<RoadEdge> availableEdges =
                roadEdgeRepository.findByBlockedFalse();

        return computeRoute(start, destination, availableEdges, buildEdgeLookup(availableEdges));
    }

    @Override
    public RouteResponse findRoute(String startNodeName, String destinationNodeName) {
        RoadNode start = roadNodeRepository.findByName(startNodeName)
                .orElseThrow(() -> new LocationNotFoundException("Start location not found"));
        RoadNode destination = roadNodeRepository.findByName(destinationNodeName)
                .orElseThrow(() -> new LocationNotFoundException("Destination location not found"));

        List<RoadEdge> availableEdges = roadEdgeRepository.findByBlockedFalse();

        return computeRoute(start, destination, availableEdges, buildEdgeLookup(availableEdges));
    }

    /**
     * Loads the unblocked road network once and hands back a snapshot that answers repeated
     * findRoute calls against it without any further database access - unlike the single-call
     * findRoute methods above, which each pay their own fresh fetch of the full node/edge lists.
     */
    @Override
    public RoutingSnapshot loadSnapshot() {
        List<RoadEdge> edges = roadEdgeRepository.findByBlockedFalse();
        Map<EdgeKey, RoadEdge> edgeLookup = buildEdgeLookup(edges);
        Map<String, RoadNode> nodesByName = roadNodeRepository.findAll().stream()
                .collect(Collectors.toMap(RoadNode::getName, Function.identity(), (a, b) -> a));

        return (startNodeName, destinationNodeName) -> {
            RoadNode start = nodesByName.get(startNodeName);
            if (start == null) {
                throw new LocationNotFoundException("Start location not found");
            }
            RoadNode destination = nodesByName.get(destinationNodeName);
            if (destination == null) {
                throw new LocationNotFoundException("Destination location not found");
            }
            return computeRoute(start, destination, edges, edgeLookup);
        };
    }

    /**
     * Runs A* and totals travel time/distance in a single pass over the resulting route, via an
     * O(1) lookup into edgeLookup - as opposed to scanning the full edge list per hop, twice
     * (once for time, once for distance), which the naive approach would cost.
     */
    private RouteResponse computeRoute(
            RoadNode start,
            RoadNode destination,
            List<RoadEdge> edges,
            Map<EdgeKey, RoadEdge> edgeLookup) {

        List<RoadNode> route = aStarAlgorithm.findShortestPath(start, destination, edges);

        if (route.isEmpty()) {
            throw new RouteNotFoundException(
                    "No available route found between the selected locations");
        }

        double totalTravelTime = 0;
        double totalDistance = 0;

        for (int i = 0; i < route.size() - 1; i++) {
            RoadEdge edge = edgeLookup.get(new EdgeKey(route.get(i).getId(), route.get(i + 1).getId()));
            if (edge != null) {
                totalTravelTime += edge.getTravelTimeMinutes();
                totalDistance += edge.getDistanceKm();
            }
        }

        return new RouteResponse("ASTAR", totalTravelTime, totalDistance, route);
    }

    private Map<EdgeKey, RoadEdge> buildEdgeLookup(List<RoadEdge> edges) {
        Map<EdgeKey, RoadEdge> lookup = new HashMap<>();
        for (RoadEdge edge : edges) {
            lookup.putIfAbsent(new EdgeKey(edge.getFromNode().getId(), edge.getToNode().getId()), edge);
        }
        return lookup;
    }

    private record EdgeKey(Long fromNodeId, Long toNodeId) {}
}