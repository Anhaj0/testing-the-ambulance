package com.ambulance.dispatch_system.resource_allocation.optimization;

import com.ambulance.dispatch_system.common.entity.Ambulance;
import com.ambulance.dispatch_system.common.entity.enums.AmbulanceStatus;
import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;
import com.ambulance.dispatch_system.common.repository.AmbulanceRepository;
import com.ambulance.dispatch_system.common.repository.RoadNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class GreedySchedulerTest {

    private AmbulanceRepository ambulanceRepository;
    private FitnessEvaluator fitnessEvaluator;
    private RoadNodeRepository roadNodeRepository;
    private GreedyScheduler greedyScheduler;

    @BeforeEach
    void setUp() {
        ambulanceRepository = Mockito.mock(AmbulanceRepository.class);
        com.ambulance.dispatch_system.routing.service.RouteService routeService = Mockito.mock(com.ambulance.dispatch_system.routing.service.RouteService.class);
        fitnessEvaluator = new FitnessEvaluator(routeService);
        greedyScheduler = new GreedyScheduler(ambulanceRepository, fitnessEvaluator);

        // Tell the mock evaluator to give AMB-01 a better score.
        com.ambulance.dispatch_system.routing.dto.RouteResponse res1 = new com.ambulance.dispatch_system.routing.dto.RouteResponse("ASTAR", 10.0, 5.0, List.of());
        com.ambulance.dispatch_system.routing.dto.RouteResponse res2 = new com.ambulance.dispatch_system.routing.dto.RouteResponse("ASTAR", 25.0, 10.0, List.of());
        
        when(routeService.findRoute("Node_B", "Node_A")).thenReturn(res1);
        when(routeService.findRoute("Node_C", "Node_A")).thenReturn(res2);
        when(routeService.loadSnapshot()).thenReturn(routeService::findRoute);
    }

    @Test
    void testFindBestAmbulance_Success() {
        Ambulance amb1 = new Ambulance();
        amb1.setVehicleNumber("AMB-01");
        amb1.setEquipment(Set.of(MedicalEquipment.ECG_MONITOR));
        amb1.setCurrentLocationNode("Node_B");

        Ambulance amb2 = new Ambulance();
        amb2.setVehicleNumber("AMB-02");
        amb2.setEquipment(Set.of(MedicalEquipment.ECG_MONITOR, MedicalEquipment.DEFIBRILLATOR));
        amb2.setCurrentLocationNode("Node_C");

        when(ambulanceRepository.findByStatus(AmbulanceStatus.AVAILABLE)).thenReturn(List.of(amb1, amb2));

        // Run the method
        Optional<Ambulance> result = greedyScheduler.findBestAmbulance("Node_A", Set.of(MedicalEquipment.ECG_MONITOR));

        // Verify the result (AMB-01 should win)
        assertTrue(result.isPresent());
        assertEquals("AMB-01", result.get().getVehicleNumber());
    }
}