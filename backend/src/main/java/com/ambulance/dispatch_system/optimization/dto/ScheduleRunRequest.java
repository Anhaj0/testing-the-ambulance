package com.ambulance.dispatch_system.optimization.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request body for triggering a scheduling run.
 *
 * @param weekStarting   the Monday of the scheduling week to build a roster for (required)
 * @param algorithm      which algorithm to run; defaults to the Genetic Algorithm (GREEDY and BOTH are /compare-only)
 * @param randomSeed     seeds the GA's Random for a reproducible run; omit for a fresh random run
 * @param gaParameters   optional GAParameters overrides; any omitted field uses the documented default
 * @param fitnessWeights optional FitnessWeights overrides; any omitted field uses the documented default
 * @param persist        whether to save the resulting roster (replacing any existing one for the week); defaults to true
 */
public record ScheduleRunRequest(
        @NotNull LocalDate weekStarting,
        AlgorithmType algorithm,
        Long randomSeed,
        GAParametersOverride gaParameters,
        FitnessWeightsOverride fitnessWeights,
        Boolean persist
) {
    public AlgorithmType algorithmOrDefault() {
        return algorithm != null ? algorithm : AlgorithmType.GENETIC_ALGORITHM;
    }

    public boolean shouldPersist() {
        return persist == null || persist;
    }
}
