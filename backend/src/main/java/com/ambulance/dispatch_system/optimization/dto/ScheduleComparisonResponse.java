package com.ambulance.dispatch_system.optimization.dto;

/** Side-by-side Genetic Algorithm vs. Greedy baseline comparison, for the experimental-evaluation chapter. */
public record ScheduleComparisonResponse(
        ScheduleRunResponse geneticAlgorithm,
        ScheduleRunResponse greedy
) {
}
