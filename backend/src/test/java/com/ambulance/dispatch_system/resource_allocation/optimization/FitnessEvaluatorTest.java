package com.ambulance.dispatch_system.resource_allocation.optimization;

import com.ambulance.dispatch_system.common.entity.Ambulance;
import com.ambulance.dispatch_system.common.entity.RoadEdge;
import com.ambulance.dispatch_system.common.entity.RoadNode;
import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;
import com.ambulance.dispatch_system.routing.service.RouteService;
import com.ambulance.dispatch_system.routing.dto.RouteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FitnessEvaluatorTest {

    private RouteService routeService;
    private FitnessEvaluator fitnessEvaluator;

    @BeforeEach
    void setUp() {
        routeService = Mockito.mock(RouteService.class);
        fitnessEvaluator = new FitnessEvaluator(routeService);
    }

    @Test
    void testCalculateFitness_ZeroDistance_NoExtraEquipment() {
        Ambulance ambulance = new Ambulance();
        ambulance.setCurrentLocationNode("NodeA");
        ambulance.setEquipment(Set.of(MedicalEquipment.DEFIBRILLATOR));

        RouteResponse response = new RouteResponse();
        response.setTotalTravelTimeMinutes(0.0);
        when(routeService.findRoute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(response);

        RoadNode nodeA = new RoadNode();
        nodeA.setId(1L);
        nodeA.setName("NodeA");

        double score = fitnessEvaluator.calculateFitness(ambulance, "NodeA", 
                Set.of(MedicalEquipment.DEFIBRILLATOR));

        assertEquals(0.0, score);
    }

    @Test
    void testCalculateFitness_WithDistanceAndExtraEquipment() {
        Ambulance ambulance = new Ambulance();
        ambulance.setCurrentLocationNode("NodeA");
        ambulance.setEquipment(Set.of(MedicalEquipment.DEFIBRILLATOR, MedicalEquipment.VENTILATOR));

        RouteResponse response = new RouteResponse();
        response.setTotalTravelTimeMinutes(15.5);
        when(routeService.findRoute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(response);

        RoadNode nodeA = new RoadNode();
        nodeA.setId(1L);
        nodeA.setName("NodeA");
        
        RoadNode nodeB = new RoadNode();
        nodeB.setId(2L);
        nodeB.setName("NodeB");

        // 1 extra equipment = 5.0 penalty + 15.5 distance = 20.5
        double score = fitnessEvaluator.calculateFitness(ambulance, "NodeB", 
                Set.of(MedicalEquipment.DEFIBRILLATOR));

        assertEquals(20.5, score);
    }
}
