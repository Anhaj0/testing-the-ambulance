package com.ambulance.dispatch_system.network_detection.service;

import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.common.entity.enums.AmbulanceStatus;
import com.ambulance.dispatch_system.common.repository.AmbulanceRepository;
import com.ambulance.dispatch_system.common.repository.RoadEdgeRepository;
import com.ambulance.dispatch_system.common.repository.RoadNodeRepository;
import com.ambulance.dispatch_system.network_detection.optimization.DijkstraBlindSpotOptimizer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NetworkAnalysisService {

    private final RoadNodeRepository nodeRepository;
    private final RoadEdgeRepository edgeRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final DijkstraBlindSpotOptimizer blindSpotOptimizer;

    public NetworkAnalysisService(RoadNodeRepository nodeRepository,
                                  RoadEdgeRepository edgeRepository,
                                  AmbulanceRepository ambulanceRepository,
                                  DijkstraBlindSpotOptimizer blindSpotOptimizer) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.blindSpotOptimizer = blindSpotOptimizer;
    }

    public List<RoadNode> findBlindSpots(double thresholdMinutes) {
        return computeReachability().blindSpots(thresholdMinutes);
    }

    /**
     * Loads the road network and runs one multi-source Dijkstra pass from every available
     * ambulance base, without applying a threshold. Callers that need to check several
     * thresholds (see NetworkCoverageStatsService) should call this once and filter the result
     * repeatedly, rather than calling findBlindSpots per threshold - each of those calls would
     * otherwise re-fetch the whole road network and re-run the traversal from scratch.
     */
    public NetworkReachability computeReachability() {
        var allNodes = nodeRepository.findAll();
        var allEdges = edgeRepository.findAll();

        Set<String> baseNodeNames = ambulanceRepository.findAll().stream()
                .filter(amb -> amb.getStatus() == AmbulanceStatus.AVAILABLE)
                .map(amb -> amb.getCurrentLocationNode())
                .filter(nodeName -> nodeName != null && !nodeName.isBlank())
                .collect(Collectors.toSet());

        Map<Long, Double> minTravelTimes = blindSpotOptimizer.computeMinTravelTimes(allNodes, allEdges, baseNodeNames);
        return new NetworkReachability(allNodes, minTravelTimes);
    }
}