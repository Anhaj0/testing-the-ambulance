package com.ambulance.dispatch_system.optimization.dto;

import com.ambulance.dispatch_system.optimization.fitness.FitnessResult;

import java.util.List;

/**
 * API view of one algorithm's scheduling run: the metrics needed for the
 * evaluation chapter (fitness breakdown, execution time, generation
 * count, convergence curve) plus the resulting shift assignments.
 */
public record ScheduleRunResponse(
        String algorithmName,
        FitnessResult fitnessResult,
        long executionTimeMillis,
        int generationsRun,
        List<Double> bestFitnessHistory,
        List<ShiftDto> assignments
) {
}
