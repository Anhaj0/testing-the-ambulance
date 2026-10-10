package com.ambulance.dispatch_system.optimization.dto;

import com.ambulance.dispatch_system.optimization.ga.GAParameters;

/**
 * Optional per-request overrides for GAParameters. Any null field falls
 * back to GAParameters.defaults(), so a caller can tune just the one or
 * two knobs they care about (e.g. populationSize) for an experimental run.
 */
public record GAParametersOverride(
        Integer populationSize,
        Integer maxGenerations,
        Double crossoverRate,
        Double mutationRate,
        Integer elitismCount,
        Integer tournamentSize,
        Double convergenceThreshold,
        Integer convergenceWindow
) {
    public GAParameters applyTo(GAParameters defaults) {
        return new GAParameters(
                populationSize != null ? populationSize : defaults.populationSize(),
                maxGenerations != null ? maxGenerations : defaults.maxGenerations(),
                crossoverRate != null ? crossoverRate : defaults.crossoverRate(),
                mutationRate != null ? mutationRate : defaults.mutationRate(),
                elitismCount != null ? elitismCount : defaults.elitismCount(),
                tournamentSize != null ? tournamentSize : defaults.tournamentSize(),
                convergenceThreshold != null ? convergenceThreshold : defaults.convergenceThreshold(),
                convergenceWindow != null ? convergenceWindow : defaults.convergenceWindow());
    }
}
