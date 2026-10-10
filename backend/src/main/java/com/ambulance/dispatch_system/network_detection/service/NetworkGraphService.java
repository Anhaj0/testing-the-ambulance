package com.ambulance.dispatch_system.network_detection.service;

import com.ambulance.dispatch_system.common.entity.Ambulance;
import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.common.repository.AmbulanceRepository;
import com.ambulance.dispatch_system.common.repository.RoadEdgeRepository;
import com.ambulance.dispatch_system.common.repository.RoadNodeRepository;
import com.ambulance.dispatch_system.network_detection.dto.AmbulanceMarkerDto;
import com.ambulance.dispatch_system.network_detection.dto.RoadEdgeDto;
import com.ambulance.dispatch_system.network_detection.dto.RoadNodeDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NetworkGraphService {

    private final RoadNodeRepository nodeRepository;
    private final RoadEdgeRepository edgeRepository;
    private final AmbulanceRepository ambulanceRepository;

    public NetworkGraphService(RoadNodeRepository nodeRepository,
                                RoadEdgeRepository edgeRepository,
                                AmbulanceRepository ambulanceRepository) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.ambulanceRepository = ambulanceRepository;
    }

    public List<RoadNodeDto> getAllNodes() {
        return nodeRepository.findAll().stream()
                .map(n -> new RoadNodeDto(n.getId(), n.getName(), n.getLatitude(), n.getLongitude()))
                .collect(Collectors.toList());
    }

    public List<RoadEdgeDto> getAllEdges() {
        return edgeRepository.findAll().stream()
                .map(e -> new RoadEdgeDto(
                        e.getId(),
                        e.getFromNode() != null ? e.getFromNode().getName() : null,
                        e.getToNode() != null ? e.getToNode().getName() : null,
                        e.getDistanceKm(),
                        e.getTravelTimeMinutes(),
                        e.isBlocked()))
                .collect(Collectors.toList());
    }

    public List<AmbulanceMarkerDto> getAllAmbulanceMarkers() {
        List<RoadNode> allNodes = nodeRepository.findAll();
        Map<String, RoadNode> nodesByName = allNodes.stream()
                .collect(Collectors.toMap(RoadNode::getName, Function.identity(), (a, b) -> a));

        List<Ambulance> ambulances = ambulanceRepository.findAll();

        return ambulances.stream()
                .map(amb -> {
                    RoadNode loc = amb.getCurrentLocationNode() != null
                            ? nodesByName.get(amb.getCurrentLocationNode())
                            : null;
                    return new AmbulanceMarkerDto(
                            amb.getId(),
                            amb.getVehicleNumber(),
                            amb.getCurrentLocationNode(),
                            amb.getStatus() != null ? amb.getStatus().name() : null,
                            loc != null ? loc.getLatitude() : null,
                            loc != null ? loc.getLongitude() : null
                    );
                })
                .collect(Collectors.toList());
    }
}