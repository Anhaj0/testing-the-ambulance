package com.ambulance.dispatch_system.resource_allocation.controller;

import com.ambulance.dispatch_system.resource_allocation.dto.AmbulanceDto;
import com.ambulance.dispatch_system.resource_allocation.dto.CallDto;
import com.ambulance.dispatch_system.resource_allocation.dto.CandidateDto;
import com.ambulance.dispatch_system.resource_allocation.dto.DispatchResultDto;
import com.ambulance.dispatch_system.resource_allocation.exception.CallNotFoundException;
import com.ambulance.dispatch_system.resource_allocation.service.DispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchControllerTest {

    @Mock
    private DispatchService dispatchService;

    @InjectMocks
    private DispatchController dispatchController;

    @Test
    void allocateAmbulance_Success_ReturnsOk() {
        Long callId = 1L;
        DispatchResultDto expected = new DispatchResultDto(true, callId, "AMB-001",
                "Ambulance AMB-001 dispatched successfully.");
        when(dispatchService.handleEmergencyDispatch(callId)).thenReturn(expected);

        ResponseEntity<DispatchResultDto> response = dispatchController.allocateAmbulance(callId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void allocateAmbulance_CallNotFound_PropagatesToGlobalExceptionHandler() {
        Long callId = 1L;
        when(dispatchService.handleEmergencyDispatch(callId)).thenThrow(new CallNotFoundException(callId));

        // The controller no longer catches this itself - GlobalExceptionHandler maps
        // CallNotFoundException (a BaseException) to the 404 response.
        assertThrows(CallNotFoundException.class, () -> dispatchController.allocateAmbulance(callId));
    }

    @Test
    void getCandidates_ReturnsRankedList() {
        Long callId = 1L;
        List<CandidateDto> candidates = List.of(new CandidateDto(2L, "AMB-002", 7.0, 1, 12.0));
        when(dispatchService.getCandidates(callId)).thenReturn(candidates);

        ResponseEntity<List<CandidateDto>> response = dispatchController.getCandidates(callId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(candidates, response.getBody());
    }

    @Test
    void getPendingEmergencies_ReturnsList() {
        List<CallDto> pendingCalls = List.of(
                new CallDto(1L, "Jane Doe", null, "N1", null, null, null, null, 7L, 35));
        when(dispatchService.getPendingCalls()).thenReturn(pendingCalls);

        ResponseEntity<List<CallDto>> response = dispatchController.getPendingEmergencies();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pendingCalls, response.getBody());
    }

    @Test
    void getAmbulances_ReturnsList() {
        List<AmbulanceDto> ambulances = List.of(
                new AmbulanceDto(1L, "AMB-001", "N1", null, null));
        when(dispatchService.getAllAmbulances()).thenReturn(ambulances);

        ResponseEntity<List<AmbulanceDto>> response = dispatchController.getAmbulances();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ambulances, response.getBody());
    }
}
