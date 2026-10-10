package com.ambulance.dispatch_system.optimization.dto;

import com.ambulance.dispatch_system.optimization.fitness.FitnessWeights;

/**
 * Optional per-request overrides for FitnessWeights. Any null field falls
 * back to FitnessWeights.defaults(), letting a caller re-weight, say,
 * fairness vs. overtime for an experimental comparison run.
 */
public record FitnessWeightsOverride(
        Double understaffedPenalty,
        Double overtimePenaltyPerHour,
        Double restViolationPenalty,
        Double fairnessWeight,
        Double minRestHours
) {
    public FitnessWeights applyTo(FitnessWeights defaults) {
        return new FitnessWeights(
                understaffedPenalty != null ? understaffedPenalty : defaults.understaffedPenalty(),
                overtimePenaltyPerHour != null ? overtimePenaltyPerHour : defaults.overtimePenaltyPerHour(),
                restViolationPenalty != null ? restViolationPenalty : defaults.restViolationPenalty(),
                fairnessWeight != null ? fairnessWeight : defaults.fairnessWeight(),
                minRestHours != null ? minRestHours : defaults.minRestHours());
    }
}
