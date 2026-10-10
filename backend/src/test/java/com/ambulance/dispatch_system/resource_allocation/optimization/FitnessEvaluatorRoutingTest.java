package com.ambulance.dispatch_system.resource_allocation.optimization;

import com.ambulance.dispatch_system.common.entity.Ambulance;
import com.ambulance.dispatch_system.common.entity.RoadEdge;
import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;
import com.ambulance.dispatch_system.routing.service.RouteService;
import com.ambulance.dispatch_system.routing.service.RouteServiceImpl;
import com.ambulance.dispatch_system.common.repository.RoadNodeRepository;
import com.ambulance.dispatch_system.common.repository.RoadEdgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Exercises FitnessEvaluator against a real road graph and a real Dijkstra run,
 * rather than a stubbed travel time. Issue #9 was about the distance being
 * fabricated, so these cases only mean something if nothing is mocked out.
 *
 * Graph used throughout (travel time in minutes):
 *
 * NodeA --3.0--> NodeB --4.0--> NodeC NodeD (no edges)
 * \-----------1.0 (blocked)-----^
 *
 * So the only usable NodeA -> NodeC route costs 7.0.
 */
class FitnessEvaluatorRoutingTest {

    private static final double DELTA = 1e-9;
    private static final Set<MedicalEquipment> NEEDS_ECG = Set.of(MedicalEquipment.ECG_MONITOR);

    private RoadNode nodeA, nodeB, nodeC, nodeD;
    private List<RoadNode> allNodes;
    private List<RoadEdge> allEdges;
    private FitnessEvaluator evaluator;

    private RoadEdgeRepository roadEdgeRepository;

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

        RoadNodeRepository roadNodeRepository = Mockito.mock(RoadNodeRepository.class);
        roadEdgeRepository = Mockito.mock(RoadEdgeRepository.class);

        when(roadNodeRepository.findById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return allNodes.stream().filter(n -> n.getId().equals(id)).findFirst();
        });
        when(roadNodeRepository.findByName(any())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return allNodes.stream().filter(n -> n.getName().equals(name)).findFirst();
        });
        when(roadEdgeRepository.findByBlockedFalse())
                .thenAnswer(inv -> allEdges.stream().filter(e -> !e.isBlocked()).collect(Collectors.toList()));

        evaluator = new FitnessEvaluator(new RouteServiceImpl(roadNodeRepository, roadEdgeRepository));
    }

    @Test
    void usesTheRealShortestPathThroughTheRoadGraph() {
        double score = evaluator.calculateFitness(
                ambulance("AMB-01", "NodeA", NEEDS_ECG), "NodeC", NEEDS_ECG);

        // 3.0 (A->B) + 4.0 (B->C), not the blocked 1.0 shortcut and not a random
        // number.
        assertEquals(7.0, score, DELTA);
    }

    @Test
    void takesTheDirectRouteOnceItIsNoLongerBlocked() {
        allEdges = List.of(
                edge(nodeA, nodeB, 3.0, false),
                edge(nodeB, nodeC, 4.0, false),
                edge(nodeA, nodeC, 1.0, false));

        double score = evaluator.calculateFitness(
                ambulance("AMB-01", "NodeA", NEEDS_ECG), "NodeC", NEEDS_ECG);

        assertEquals(1.0, score, DELTA);
    }

    @Test
    void addsTheEquipmentPenaltyOnTopOfRealTravelTime() {
        Ambulance overEquipped = ambulance("AMB-02", "NodeA",
                Set.of(MedicalEquipment.ECG_MONITOR, MedicalEquipment.DEFIBRILLATOR));

        double score = evaluator.calculateFitness(overEquipped, "NodeC", NEEDS_ECG);

        // 7.0 travel + one unused item * 5.0
        assertEquals(12.0, score, DELTA);
    }

    @Test
    void anAmbulanceAlreadyAtThePatientCostsNothing() {
        double score = evaluator.calculateFitness(
                ambulance("AMB-03", "NodeC", NEEDS_ECG), "NodeC", NEEDS_ECG);

        assertEquals(0.0, score, DELTA);
    }

    @Test
    void ambulanceWithNoKnownLocationIsUnreachable() {
        double score = evaluator.calculateFitness(
                ambulance("AMB-GHOST", null, NEEDS_ECG), "NodeC", NEEDS_ECG);

        assertEquals(FitnessEvaluator.UNREACHABLE, score,
                "a vehicle with no recorded position must not score as if it were already there");
    }

    @Test
    void callWithNoLocationIsUnreachable() {
        double score = evaluator.calculateFitness(
                ambulance("AMB-01", "NodeA", NEEDS_ECG), null, NEEDS_ECG);

        assertEquals(FitnessEvaluator.UNREACHABLE, score);
    }

    @Test
    void unknownPatientNodeIsUnreachable() {
        double score = evaluator.calculateFitness(
                ambulance("AMB-01", "NodeA", NEEDS_ECG), "NoSuchNode", NEEDS_ECG);

        assertEquals(FitnessEvaluator.UNREACHABLE, score);
    }

    @Test
    void patientOnADisconnectedNodeIsUnreachable() {
        double score = evaluator.calculateFitness(
                ambulance("AMB-01", "NodeA", NEEDS_ECG), "NodeD", NEEDS_ECG);

        assertEquals(FitnessEvaluator.UNREACHABLE, score);
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
