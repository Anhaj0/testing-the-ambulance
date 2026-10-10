package com.ambulance.dispatch_system.optimization.dto;

import com.ambulance.dispatch_system.optimization.fitness.FitnessWeights;
import com.ambulance.dispatch_system.optimization.ga.GAParameters;

/** The default tunable parameters, exposed so callers know what's available to override and what "default" means. */
public record ScheduleDefaultsResponse(GAParameters gaParameters, FitnessWeights fitnessWeights) {
}
