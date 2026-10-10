package com.ambulance.dispatch_system.optimization.controller;

import com.ambulance.dispatch_system.optimization.dto.StaffDto;
import com.ambulance.dispatch_system.optimization.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** CRUD endpoints for Staff - the pool of doctors/drivers the scheduling algorithms assign shifts to. */
@RestController
@RequestMapping("/api/v1/optimization/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public List<StaffDto> findAll() {
        return staffService.findAll();
    }

    @GetMapping("/{id}")
    public StaffDto findById(@PathVariable Long id) {
        return staffService.findById(id);
    }

    @PostMapping
    public ResponseEntity<StaffDto> create(@Valid @RequestBody StaffDto request) {
        StaffDto created = staffService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public StaffDto update(@PathVariable Long id, @Valid @RequestBody StaffDto request) {
        return staffService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        staffService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
