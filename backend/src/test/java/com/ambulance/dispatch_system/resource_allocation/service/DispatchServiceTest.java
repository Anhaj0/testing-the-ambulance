package com.ambulance.dispatch_system.resource_allocation.service;

import com.ambulance.dispatch_system.common.entity.Ambulance;
import com.ambulance.dispatch_system.common.entity.Call;
import com.ambulance.dispatch_system.common.entity.Patient;
import com.ambulance.dispatch_system.common.entity.enums.AmbulanceStatus;
import com.ambulance.dispatch_system.common.entity.enums.CallStatus;
import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;
import com.ambulance.dispatch_system.common.repository.AmbulanceRepository;
import com.ambulance.dispatch_system.common.repository.CallRepository;
import com.ambulance.dispatch_system.resource_allocation.dto.AmbulanceDto;
import com.ambulance.dispatch_system.resource_allocation.dto.CallDto;
import com.ambulance.dispatch_system.resource_allocation.dto.CandidateDto;
import com.ambulance.dispatch_system.resource_allocation.dto.DispatchResultDto;
import com.ambulance.dispatch_system.resource_allocation.optimization.FitnessEvaluator;
import com.ambulance.dispatch_system.resource_allocation.optimization.GreedyScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private CallRepository callRepository;

    @Mock
    private AmbulanceRepository ambulanceRepository;

    @Mock
    private GreedyScheduler greedyScheduler;

    @InjectMocks
    private DispatchService dispatchService;

    @Test
    void handleEmergencyDispatch_CallNotFound_ThrowsException() {
        when(callRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dispatchService.handleEmergencyDispatch(1L);
        });

        assertEquals("Call not found with ID: 1", exception.getMessage());
    }

    @Test
    void handleEmergencyDispatch_NoAmbulanceAvailable_ReturnsMessage() {
        Call call = new Call();
        call.setId(1L);
        call.setLocationNode("Node1");
        call.setRequiredEquipment(Set.of(MedicalEquipment.DEFIBRILLATOR));

        when(callRepository.findById(1L)).thenReturn(Optional.of(call));
        when(greedyScheduler.findBestAmbulance(any(), any())).thenReturn(Optional.empty());

        DispatchResultDto result = dispatchService.handleEmergencyDispatch(1L);

        assertFalse(result.dispatched());
        assertEquals(1L, result.callId());
        assertNull(result.ambulanceVehicleNumber());
        assertEquals("No suitable ambulance available at this time.", result.message());
    }

    @Test
    void handleEmergencyDispatch_Success_DispatchesAmbulance() {
        Call call = new Call();
        call.setId(1L);
        call.setLocationNode("Node1");
        call.setRequiredEquipment(Set.of(MedicalEquipment.DEFIBRILLATOR));

        Ambulance ambulance = new Ambulance();
        ambulance.setId(1L);
        ambulance.setVehicleNumber("AMB-001");
        ambulance.setStatus(AmbulanceStatus.AVAILABLE);

        when(callRepository.findById(1L)).thenReturn(Optional.of(call));
        when(greedyScheduler.findBestAmbulance(any(), any())).thenReturn(Optional.of(ambulance));

        DispatchResultDto result = dispatchService.handleEmergencyDispatch(1L);

        assertTrue(result.dispatched());
        assertEquals(1L, result.callId());
        assertEquals("AMB-001", result.ambulanceVehicleNumber());
        assertEquals("Ambulance AMB-001 dispatched successfully.", result.message());
        assertEquals(CallStatus.DISPATCHED, call.getStatus());
        assertEquals(AmbulanceStatus.DISPATCHED, ambulance.getStatus());
        assertEquals(ambulance, call.getAssignedAmbulance());

        verify(ambulanceRepository, times(1)).save(ambulance);
        verify(callRepository, times(1)).save(call);
    }

    @Test
    void getCandidates_ReturnsRankedList() {
        Call call = new Call();
        call.setId(1L);
        call.setLocationNode("Node1");
        call.setRequiredEquipment(Set.of(MedicalEquipment.DEFIBRILLATOR));

        Ambulance ambulance = new Ambulance();
        ambulance.setId(2L);
        ambulance.setVehicleNumber("AMB-002");

        GreedyScheduler.ScoredAmbulance scored = new GreedyScheduler.ScoredAmbulance(
                ambulance, new FitnessEvaluator.FitnessBreakdown(7.0, 1, 12.0));

        when(callRepository.findById(1L)).thenReturn(Optional.of(call));
        when(greedyScheduler.rankCandidates("Node1", call.getRequiredEquipment()))
                .thenReturn(List.of(scored));

        List<CandidateDto> candidates = dispatchService.getCandidates(1L);

        assertEquals(1, candidates.size());
        CandidateDto candidate = candidates.get(0);
        assertEquals(2L, candidate.ambulanceId());
        assertEquals("AMB-002", candidate.vehicleNumber());
        assertEquals(7.0, candidate.travelMinutes());
        assertEquals(1, candidate.extraEquipmentCount());
        assertEquals(12.0, candidate.score());
    }

    @Test
    void getCandidates_CallNotFound_ThrowsException() {
        when(callRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> dispatchService.getCandidates(1L));
    }

    @Test
    void getPendingCalls_ReturnsCalls() {
        Call call1 = new Call();
        call1.setId(12L);
        Patient patient = new Patient();
        patient.setId(7L);
        patient.setName("Same-name patient");
        patient.setAge(42);
        call1.setPatient(patient);
        call1.setStatus(CallStatus.RECEIVED);

        when(callRepository.findByStatus(CallStatus.RECEIVED)).thenReturn(List.of(call1));

        List<CallDto> pendingCalls = dispatchService.getPendingCalls();

        assertNotNull(pendingCalls);
        assertEquals(1, pendingCalls.size());
        assertEquals(CallStatus.RECEIVED, pendingCalls.get(0).status());
        assertEquals(12L, pendingCalls.get(0).id());
        assertEquals(7L, pendingCalls.get(0).patientId());
        assertEquals(42, pendingCalls.get(0).patientAge());
    }

    @Test
    void getAllAmbulances_ReturnsAmbulances() {
        Ambulance ambulance1 = new Ambulance();
        when(ambulanceRepository.findAll()).thenReturn(List.of(ambulance1));

        List<AmbulanceDto> ambulances = dispatchService.getAllAmbulances();

        assertNotNull(ambulances);
        assertEquals(1, ambulances.size());
    }
}
