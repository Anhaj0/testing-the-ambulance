package com.ambulance.dispatch_system.optimization.controller;

import com.ambulance.dispatch_system.optimization.dto.ScheduleComparisonResponse;
import com.ambulance.dispatch_system.optimization.dto.ScheduleDefaultsResponse;
import com.ambulance.dispatch_system.optimization.dto.ScheduleRunRequest;
import com.ambulance.dispatch_system.optimization.dto.ScheduleRunResponse;
import com.ambulance.dispatch_system.optimization.dto.ShiftDto;
import com.ambulance.dispatch_system.optimization.fitness.FitnessWeights;
import com.ambulance.dispatch_system.optimization.ga.GAParameters;
import com.ambulance.dispatch_system.optimization.service.SchedulingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST entry point for the Optimization Module: trigger a scheduling run
 * (Genetic Algorithm or Greedy baseline), compare both algorithms
 * side by side, and view a previously generated roster.
 */
@RestController
@RequestMapping("/api/v1/optimization/schedules")
public class SchedulingController {

    private final SchedulingService schedulingService;

    public SchedulingController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    /** Runs the Genetic Algorithm and, unless persist=false, saves the resulting roster. */
    @PostMapping("/runs")
    public ScheduleRunResponse run(@Valid @RequestBody ScheduleRunRequest request) {
        return schedulingService.run(request);
    }

    /** Runs both the GA and the Greedy baseline against the same problem for the evaluation chapter. Never persists. */
    @PostMapping("/comparisons")
    public ScheduleComparisonResponse compare(@Valid @RequestBody ScheduleRunRequest request) {
        return schedulingService.compare(request);
    }

    /** Returns the persisted roster for a given week (its Monday date). */
    @GetMapping
    public List<ShiftDto> roster(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStarting) {
        return schedulingService.getRoster(weekStarting);
    }

    /** The default GA parameters and fitness weights, so callers know what's tunable and what "default" means. */
    @GetMapping("/defaults")
    public ScheduleDefaultsResponse defaults() {
        return new ScheduleDefaultsResponse(GAParameters.defaults(), FitnessWeights.defaults());
    }
}
