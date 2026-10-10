package com.ambulance.dispatch_system.resource_allocation.optimization;

import com.ambulance.dispatch_system.common.entity.Ambulance;
import com.ambulance.dispatch_system.common.entity.RoadEdge;
import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.common.entity.enums.AmbulanceStatus;
import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;
import com.ambulance.dispatch_system.common.repository.AmbulanceRepository;
import com.ambulance.dispatch_system.common.repository.RoadEdgeRepository;
import com.ambulance.dispatch_system.common.repository.RoadNodeRepository;
import com.ambulance.dispatch_system.routing.service.RouteService;
import com.ambulance.dispatch_system.routing.service.RouteServiceImpl;
import com.ambulance.dispatch_system.routing.service.RoutingSnapshot;
import com.ambulance.dispatch_system.routing.dto.RouteResponse;
import com.ambulance.dispatch_system.routing.dto.RouteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * End-to-end selection tests: only the repositories are mocked, so the scheduler,
 * the fitness function and Dijkstra all run for real against the same graph used
 * by FitnessEvaluatorRoutingTest.
 *
 *     NodeA --3.0--> NodeB --4.0--> NodeC        NodeD (no edges)
 *       \-----------1.0 (blocked)-----^
 *
 * NodeA -> NodeC costs 7.0; NodeB -> NodeC costs 4.0.
 */
class GreedySchedulerRoutingTest {

    private static final Set<MedicalEquipment> NEEDS_ECG = Set.of(MedicalEquipment.ECG_MONITOR);

    private RoadNode nodeA, nodeB, nodeC, nodeD;
    private List<RoadNode> allNodes;
    private List<RoadEdge> allEdges;

    @BeforeEach
    void setUp() {
        nodeA = node(1L, "NodeA");
        nodeB = node(2L, "NodeB");
        nodeC = node(3L, "NodeC");
        nodeD = node(4L, "NodeD");
        allNodes = List.of(nodeA, nodeB, nodeC, nodeD);
        allEdges = List.of(
                edge(nodeA, nodeB, 3.0, false),
                edge(nodeB, nodeC, 4.0, false),
                edge(nodeA, nodeC, 1.0, true));
    }

    @Test
    void picksTheGenuinelyNearestAmbulance() {
        Ambulance farther = ambulance("AMB-FAR", "NodeA", NEEDS_ECG);   // 7.0 away
        Ambulance nearer = ambulance("AMB-NEAR", "NodeB", NEEDS_ECG);   // 4.0 away

        Optional<Ambulance> best = scheduler(List.of(farther, nearer))
                .findBestAmbulance("NodeC", NEEDS_ECG);

        assertTrue(best.isPresent());
        assertEquals("AMB-NEAR", best.get().getVehicleNumber());
    }

    @Test
    void ignoresAnAmbulanceWithNoKnownLocation() {
        Ambulance ghost = ambulance("AMB-GHOST", null, NEEDS_ECG);
        Ambulance real = ambulance("AMB-REAL", "NodeA", NEEDS_ECG);

        Optional<Ambulance> best = scheduler(List.of(ghost, real))
                .findBestAmbulance("NodeC", NEEDS_ECG);

        assertTrue(best.isPresent());
        assertEquals("AMB-REAL", best.get().getVehicleNumber(),
                "a vehicle with no recorded position must not be treated as the closest");
    }

    @Test
    void returnsEmptyWhenThePatientNodeIsUnknown() {
        Optional<Ambulance> best = scheduler(List.of(ambulance("AMB-01", "NodeA", NEEDS_ECG)))
                .findBestAmbulance("NoSuchNode", NEEDS_ECG);

        assertTrue(best.isEmpty(), "an unroutable call must not report a dispatchable ambulance");
    }

    @Test
    void returnsEmptyWhenNoRouteReachesThePatient() {
        Optional<Ambulance> best = scheduler(List.of(ambulance("AMB-01", "NodeA", NEEDS_ECG)))
                .findBestAmbulance("NodeD", NEEDS_ECG);

        assertTrue(best.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoAmbulanceCarriesTheRequiredEquipment() {
        Ambulance basic = ambulance("AMB-01", "NodeA", Set.of(MedicalEquipment.DEFIBRILLATOR));

        Optional<Ambulance> best = scheduler(List.of(basic)).findBestAmbulance("NodeC", NEEDS_ECG);

        assertTrue(best.isEmpty());
    }

    @Test
    void returnsEmptyWhenNoAmbulanceIsAvailable() {
        Optional<Ambulance> best = scheduler(List.of()).findBestAmbulance("NodeC", NEEDS_ECG);

        assertTrue(best.isEmpty());
    }

    @Test
    void computesTheShortestPathOncePerAmbulance() {
        int[] runs = {0};
        RouteService counting = new RouteService() {
            @Override
            public RouteResponse findRoute(Long startId, Long endId) {
                return null;
            }
            @Override
            public RouteResponse findRoute(String start, String end) {
                runs[0]++;
                RouteResponse res = new RouteResponse();
                res.setTotalTravelTimeMinutes(10.0);
                return res;
            }
            @Override
            public RouteResponse findRoute(RouteRequest request) {
                return findRoute(request.getStartLocationId(), request.getDestinationLocationId());
            }
            @Override
            public RoutingSnapshot loadSnapshot() {
                // No caching to test - just delegate through the instrumented findRoute above,
                // so the "once per ambulance" count below still reflects every scoring call.
                return this::findRoute;
            }
        };
        List<Ambulance> fleet = List.of(
                ambulance("A1", "NodeA", NEEDS_ECG), ambulance("A2", "NodeA", NEEDS_ECG),
                ambulance("A3", "NodeA", NEEDS_ECG), ambulance("A4", "NodeB", NEEDS_ECG),
                ambulance("A5", "NodeB", NEEDS_ECG));

        scheduler(fleet, counting).findBestAmbulance("NodeC", NEEDS_ECG);

        assertEquals(fleet.size(), runs[0],
                "scoring must not re-run the graph search on both sides of every comparison");
    }

    @Test
    void fetchesTheRoadNetworkOnceRegardlessOfFleetSize() {
        RoadNodeRepository roadNodeRepository = Mockito.mock(RoadNodeRepository.class);
        RoadEdgeRepository roadEdgeRepository = Mockito.mock(RoadEdgeRepository.class);

        when(roadNodeRepository.findByName(Mockito.any())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return allNodes.stream().filter(n -> n.getName().equals(name)).findFirst();
        });
        when(roadNodeRepository.findAll()).thenReturn(allNodes);
        when(roadEdgeRepository.findByBlockedFalse()).thenAnswer(inv ->
                allEdges.stream().filter(e -> !e.isBlocked()).toList());

        AmbulanceRepository ambulanceRepository = Mockito.mock(AmbulanceRepository.class);
        List<Ambulance> fleet = List.of(
                ambulance("A1", "NodeA", NEEDS_ECG), ambulance("A2", "NodeA", NEEDS_ECG),
                ambulance("A3", "NodeA", NEEDS_ECG), ambulance("A4", "NodeB", NEEDS_ECG),
                ambulance("A5", "NodeB", NEEDS_ECG));
        when(ambulanceRepository.findByStatus(AmbulanceStatus.AVAILABLE)).thenReturn(fleet);

        RouteService routeService = new RouteServiceImpl(roadNodeRepository, roadEdgeRepository);
        GreedyScheduler scheduler = new GreedyScheduler(ambulanceRepository, new FitnessEvaluator(routeService));

        scheduler.findBestAmbulance("NodeC", NEEDS_ECG);

        Mockito.verify(roadEdgeRepository, Mockito.times(1)).findByBlockedFalse();
        Mockito.verify(roadNodeRepository, Mockito.times(1)).findAll();
    }

    private GreedyScheduler scheduler(List<Ambulance> available) {
        RoadNodeRepository roadNodeRepository = Mockito.mock(RoadNodeRepository.class);
        RoadEdgeRepository roadEdgeRepository = Mockito.mock(RoadEdgeRepository.class);
        
        when(roadNodeRepository.findById(Mockito.any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return allNodes.stream().filter(n -> n.getId().equals(id)).findFirst();
        });
        when(roadNodeRepository.findByName(Mockito.any())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return allNodes.stream().filter(n -> n.getName().equals(name)).findFirst();
        });
        when(roadNodeRepository.findAll()).thenReturn(allNodes);
        when(roadEdgeRepository.findByBlockedFalse()).thenAnswer(inv ->
            allEdges.stream().filter(e -> !e.isBlocked()).toList()
        );

        return scheduler(available, new RouteServiceImpl(roadNodeRepository, roadEdgeRepository));
    }

    private GreedyScheduler scheduler(List<Ambulance> available, RouteService routeService) {
        AmbulanceRepository ambulanceRepository = Mockito.mock(AmbulanceRepository.class);
        RoadNodeRepository roadNodeRepository = Mockito.mock(RoadNodeRepository.class);

        when(ambulanceRepository.findByStatus(AmbulanceStatus.AVAILABLE)).thenReturn(available);
        when(roadNodeRepository.findAll()).thenReturn(allNodes);

        return new GreedyScheduler(ambulanceRepository, new FitnessEvaluator(routeService));
    }

    private RoadNode node(Long id, String name) {
        RoadNode n = new RoadNode();
        n.setId(id);
        n.setName(name);
        return n;
    }

    private RoadEdge edge(RoadNode from, RoadNode to, double minutes, boolean blocked) {
        RoadEdge e = new RoadEdge();
        e.setFromNode(from);
        e.setToNode(to);
        e.setTravelTimeMinutes(minutes);
        e.setBlocked(blocked);
        return e;
    }

    private Ambulance ambulance(String vehicleNumber, String location, Set<MedicalEquipment> equipment) {
        Ambulance a = new Ambulance();
        a.setVehicleNumber(vehicleNumber);
        a.setCurrentLocationNode(location);
        a.setEquipment(equipment);
        return a;
    }
}
