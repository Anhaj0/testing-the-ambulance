package com.ambulance.dispatch_system.optimization.dto;

/** Which scheduling algorithm(s) to run for a given request. */
public enum AlgorithmType {
    GENETIC_ALGORITHM,
    /** Only valid on the /compare endpoint - runs as the baseline against the Genetic Algorithm. */
    GREEDY,
    /** Only valid on the /compare endpoint - runs both against the same problem. */
    BOTH
}
