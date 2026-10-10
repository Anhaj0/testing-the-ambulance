package com.ambulance.dispatch_system.optimization.fitness;

/**
 * Breakdown of one RosterChromosome's score, kept alongside the single
 * scalar fitness value so the evaluation chapter can report each
 * constraint violation type separately.
 *
 * @param understaffedViolations number of seats left unfilled or filled by an unqualified staff member
 * @param overtimeHours          total hours worked beyond each staff member's maxWeeklyHours, summed across staff
 * @param restViolations         number of consecutive-shift gaps shorter than the configured minimum rest period
 * @param fairnessStdDevHours    population standard deviation of hours worked across the staff pool
 * @param totalPenalty           weighted sum of the violations above (lower is better)
 * @param fitness                {@code -totalPenalty}; the value the GA maximises (higher is better)
 */
public record FitnessResult(
        int understaffedViolations,
        double overtimeHours,
        int restViolations,
        double fairnessStdDevHours,
        double totalPenalty,
        double fitness
) {
}
