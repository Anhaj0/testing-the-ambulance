package com.ambulance.dispatch_system.optimization.fitness;

/**
 * Tunable penalty weights for the shift-scheduling fitness function,
 * plus the minimum rest period (in hours) required between two shifts
 * for the same staff member. Kept as a single configuration object so
 * the weights can be varied experimentally and reported on (see
 * CLAUDE.md - Genetic Algorithm requirements).
 */
public record FitnessWeights(
        double understaffedPenalty,
        double overtimePenaltyPerHour,
        double restViolationPenalty,
        double fairnessWeight,
        double minRestHours
) {
    public static FitnessWeights defaults() {
        return new FitnessWeights(100.0, 10.0, 50.0, 1.0, 8.0);
    }
}
