package com.ambulance.dispatch_system.triage.controller;

import com.ambulance.dispatch_system.triage.dto.TriageRequestDTO;
import com.ambulance.dispatch_system.triage.dto.TriageResponseDTO;
import com.ambulance.dispatch_system.triage.service.TriageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller responsible for handling triage-related requests.
 * Provides endpoints for evaluating patients, viewing the active queue,
 * and resolving completed triage cases.
 */
@RestController
@RequestMapping("/api/v1/triage/assessments")
public class TriageController {

    private final TriageService triageService;

    public TriageController(TriageService triageService) {
        this.triageService = triageService;
    }

    /**
     * Evaluates a new triage request, creating an assessment and adding it to the triage queue.
     *
     * @param request patient triage information
     * @return generated triage response
     */
    @PostMapping
    public ResponseEntity<TriageResponseDTO> evaluate(
            @Valid @RequestBody TriageRequestDTO request) {

        TriageResponseDTO response = triageService.evaluate(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Retrieves all currently active triage cases.
     *
     * @return list of active triage cases
     */
    @GetMapping("/queue")
    public ResponseEntity<List<TriageResponseDTO>> getQueue() {
        List<TriageResponseDTO> queue = triageService.getActiveQueue();

        return ResponseEntity.ok(queue);
    }

    /**
     * Marks a triage case as resolved.
     *
     * @param id unique identifier of the triage case
     * @return empty response with HTTP 204 status
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<Void> markResolved(@PathVariable UUID id) {
        triageService.markResolved(id);

        return ResponseEntity.noContent().build();
    }
}