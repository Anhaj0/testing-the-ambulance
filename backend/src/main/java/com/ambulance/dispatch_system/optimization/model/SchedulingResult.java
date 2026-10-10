package com.ambulance.dispatch_system.optimization.model;

import com.ambulance.dispatch_system.optimization.fitness.FitnessResult;

import java.util.List;

/**
 * Outcome of running one scheduling algorithm (Genetic Algorithm or
 * Greedy baseline) against a SchedulingProblem, bundled with the
 * metrics the evaluation chapter needs: execution time, generation
 * count, and the best-fitness-per-generation convergence curve
 * (a single-entry list for the Greedy baseline, which has no
 * generations).
 */
public record SchedulingResult(
        String algorithmName,
        RosterChromosome bestChromosome,
        FitnessResult fitnessResult,
        long executionTimeMillis,
        int generationsRun,
        List<Double> bestFitnessHistory
) {
}
