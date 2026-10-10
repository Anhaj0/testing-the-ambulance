package com.ambulance.dispatch_system.optimization.controller;

import com.ambulance.dispatch_system.optimization.dto.ShiftSlotDto;
import com.ambulance.dispatch_system.optimization.service.ShiftSlotService;
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

/** CRUD endpoints for ShiftSlot - the recurring weekly coverage template the scheduling algorithms fill. */
@RestController
@RequestMapping("/api/v1/optimization/shift-slots")
public class ShiftSlotController {

    private final ShiftSlotService shiftSlotService;

    public ShiftSlotController(ShiftSlotService shiftSlotService) {
        this.shiftSlotService = shiftSlotService;
    }

    @GetMapping
    public List<ShiftSlotDto> findAll() {
        return shiftSlotService.findAll();
    }

    @GetMapping("/{id}")
    public ShiftSlotDto findById(@PathVariable Long id) {
        return shiftSlotService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ShiftSlotDto> create(@Valid @RequestBody ShiftSlotDto request) {
        ShiftSlotDto created = shiftSlotService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ShiftSlotDto update(@PathVariable Long id, @Valid @RequestBody ShiftSlotDto request) {
        return shiftSlotService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shiftSlotService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
