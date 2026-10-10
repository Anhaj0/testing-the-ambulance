package com.ambulance.dispatch_system.optimization.ga;

/**
 * Configurable Genetic Algorithm parameters, exposed as a single object
 * so population size, operator rates and stopping criteria can be
 * tuned and reported on experimentally (see CLAUDE.md - Genetic
 * Algorithm requirements).
 *
 * @param populationSize        number of chromosomes per generation
 * @param maxGenerations        hard cap on generations if convergence is never reached
 * @param crossoverRate         probability a child is produced by crossover rather than being an unmodified copy of a selected parent
 * @param mutationRate          per-gene probability of random reassignment
 * @param elitismCount          number of top chromosomes carried unchanged into the next generation
 * @param tournamentSize        number of chromosomes sampled per tournament-selection draw
 * @param convergenceThreshold  minimum improvement in best fitness over convergenceWindow generations to keep going
 * @param convergenceWindow     number of generations over which convergence is measured
 */
public record GAParameters(
        int populationSize,
        int maxGenerations,
        double crossoverRate,
        double mutationRate,
        int elitismCount,
        int tournamentSize,
        double convergenceThreshold,
        int convergenceWindow
) {
    public static GAParameters defaults() {
        return new GAParameters(50, 200, 0.8, 0.05, 2, 5, 0.001, 20);
    }
}
