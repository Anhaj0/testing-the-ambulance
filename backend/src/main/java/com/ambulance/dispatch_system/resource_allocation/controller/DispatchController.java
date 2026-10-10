package com.ambulance.dispatch_system.resource_allocation.controller;

import com.ambulance.dispatch_system.resource_allocation.dto.AmbulanceDto;
import com.ambulance.dispatch_system.resource_allocation.dto.CallDto;
import com.ambulance.dispatch_system.resource_allocation.dto.CandidateDto;
import com.ambulance.dispatch_system.resource_allocation.dto.DispatchResultDto;
import com.ambulance.dispatch_system.resource_allocation.service.DispatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller responsible for matching emergency calls to ambulances.
 * Provides endpoints to trigger dispatch for a call and to inspect the
 * current pending queue and fleet state.
 */
@RestController
@RequestMapping("/api/v1/calls")
public class DispatchController {

    private final DispatchService dispatchService;

    public DispatchController(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    /**
     * Triggers resource allocation for a specific emergency call, assigning the
     * best available ambulance if one exists.
     *
     * Errors (e.g. an unknown call id) are handled centrally by GlobalExceptionHandler.
     *
     * @param id identifier of the call to dispatch
     * @return the outcome of the dispatch attempt
     */
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<DispatchResultDto> allocateAmbulance(@PathVariable Long id) {
        DispatchResultDto result = dispatchService.handleEmergencyDispatch(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Ranks every ambulance eligible for a call, best-first, exactly as the greedy
     * scheduler would decide - without dispatching anything. Read-only; safe to poll
     * for a live preview before committing via {@link #allocateAmbulance}.
     *
     * Errors (e.g. an unknown call id) are handled centrally by GlobalExceptionHandler.
     *
     * @param id identifier of the call to rank candidates for
     * @return candidate ambulances in dispatch order
     */
    @GetMapping("/{id}/candidates")
    public ResponseEntity<List<CandidateDto>> getCandidates(@PathVariable Long id) {
        return ResponseEntity.ok(dispatchService.getCandidates(id));
    }

    /**
     * Retrieves all emergency calls still awaiting dispatch.
     *
     * @return list of pending calls
     */
    @GetMapping("/pending")
    public ResponseEntity<List<CallDto>> getPendingEmergencies() {
        return ResponseEntity.ok(dispatchService.getPendingCalls());
    }

    /**
     * Retrieves the full ambulance fleet, regardless of status.
     *
     * @return list of ambulances
     */
    @GetMapping("/ambulances")
    public ResponseEntity<List<AmbulanceDto>> getAmbulances() {
        return ResponseEntity.ok(dispatchService.getAllAmbulances());
    }
}
