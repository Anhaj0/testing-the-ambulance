package com.ambulance.dispatch_system.optimization.ga;

import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.optimization.fitness.RosterFitnessEvaluator;
import com.ambulance.dispatch_system.optimization.fitness.FitnessResult;
import com.ambulance.dispatch_system.optimization.fitness.FitnessWeights;
import com.ambulance.dispatch_system.optimization.model.RosterChromosome;
import com.ambulance.dispatch_system.optimization.model.SchedulingProblem;
import com.ambulance.dispatch_system.optimization.model.SchedulingResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Genetic Algorithm solver for the shift-scheduling optimisation
 * problem - an NP-hard constraint satisfaction problem, so an exact
 * method (e.g. ILP) becomes computationally infeasible as the roster
 * grows, which is why an approximation method is used instead (see
 * CLAUDE.md - System Scenario).
 *
 * <p>Each chromosome is a full weekly roster (a RosterChromosome).
 * Genetic operators are: tournament selection, single-point crossover,
 * per-gene random-reassignment mutation, and elitism.
 *
 * <p><b>Complexity:</b> evaluating one generation costs
 * O(P * n log n) (P chromosomes, each an O(n log n) RosterFitnessEvaluator
 * pass over n seats); producing the next generation costs O(P) for
 * selection/crossover plus O(n) per child for mutation, i.e. O(P * n).
 * Over G generations the total is O(G * P * n log n).
 */
public class GeneticAlgorithmScheduler {

    private final SchedulingProblem problem;
    private final GAParameters params;
    private final RosterFitnessEvaluator evaluator;
    private final Random random;

    /**
     * @param random inject a seeded Random (rather than {@code new Random()}) to get
     *               reproducible runs for the experimental-evaluation chapter.
     */
    public GeneticAlgorithmScheduler(SchedulingProblem problem, GAParameters params, FitnessWeights weights, Random random) {
        this.problem = problem;
        this.params = params;
        this.evaluator = new RosterFitnessEvaluator(weights);
        this.random = random;
    }

    public SchedulingResult run() {
        long startTime = System.nanoTime();

        List<RosterChromosome> population = initializePopulation();
        population.forEach(c -> evaluator.evaluate(c, problem));

        RosterChromosome best = bestOf(population);
        List<Double> bestFitnessHistory = new ArrayList<>();
        bestFitnessHistory.add(best.getFitness());

        int generationsRun = 0;
        for (int generation = 1; generation <= params.maxGenerations(); generation++) {
            population = nextGeneration(population);
            population.forEach(c -> evaluator.evaluate(c, problem));

            RosterChromosome generationBest = bestOf(population);
            if (generationBest.getFitness() > best.getFitness()) {
                best = generationBest;
            }
            bestFitnessHistory.add(best.getFitness());
            generationsRun = generation;

            if (hasConverged(bestFitnessHistory)) {
                break;
            }
        }

        long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;
        FitnessResult fitnessResult = evaluator.evaluate(best, problem);

        return new SchedulingResult(
                "Genetic Algorithm", best, fitnessResult, elapsedMillis, generationsRun, bestFitnessHistory);
    }

    private List<RosterChromosome> initializePopulation() {
        List<RosterChromosome> population = new ArrayList<>(params.populationSize());
        for (int i = 0; i < params.populationSize(); i++) {
            population.add(randomChromosome());
        }
        return population;
    }

    /** A fully random roster: every seat gets a uniformly-random staff member. */
    private RosterChromosome randomChromosome() {
        List<Staff> staffPool = problem.staffPool();
        RosterChromosome chromosome = RosterChromosome.empty(problem.expandedSlots().size());
        for (int i = 0; i < chromosome.size(); i++) {
            chromosome.setGene(i, staffPool.get(random.nextInt(staffPool.size())));
        }
        return chromosome;
    }

    private List<RosterChromosome> nextGeneration(List<RosterChromosome> current) {
        List<RosterChromosome> next = new ArrayList<>(params.populationSize());

        // Elitism: carry the fittest chromosomes forward unchanged so the best solution found never regresses.
        current.stream()
                .sorted(Comparator.comparingDouble(RosterChromosome::getFitness).reversed())
                .limit(params.elitismCount())
                .forEach(c -> next.add(c.copy()));

        while (next.size() < params.populationSize()) {
            RosterChromosome parentA = tournamentSelect(current);
            RosterChromosome parentB = tournamentSelect(current);

            RosterChromosome child = random.nextDouble() < params.crossoverRate()
                    ? crossover(parentA, parentB)
                    : parentA.copy();

            mutate(child);
            next.add(child);
        }

        return next;
    }

    /** Tournament selection: sample k random chromosomes, return the fittest of the sample. */
    private RosterChromosome tournamentSelect(List<RosterChromosome> population) {
        RosterChromosome winner = null;
        for (int i = 0; i < params.tournamentSize(); i++) {
            RosterChromosome candidate = population.get(random.nextInt(population.size()));
            if (winner == null || candidate.getFitness() > winner.getFitness()) {
                winner = candidate;
            }
        }
        return winner;
    }

    /** Single-point crossover: genes before the cut point come from parent A, the rest from parent B. */
    private RosterChromosome crossover(RosterChromosome parentA, RosterChromosome parentB) {
        int size = parentA.size();
        int cutPoint = size <= 1 ? 0 : random.nextInt(size - 1) + 1;

        RosterChromosome child = RosterChromosome.empty(size);
        for (int i = 0; i < size; i++) {
            child.setGene(i, i < cutPoint ? parentA.getGene(i) : parentB.getGene(i));
        }
        return child;
    }

    /** Mutation: independently, with probability mutationRate, reassign each gene to a random staff member. */
    private void mutate(RosterChromosome chromosome) {
        List<Staff> staffPool = problem.staffPool();
        for (int i = 0; i < chromosome.size(); i++) {
            if (random.nextDouble() < params.mutationRate()) {
                chromosome.setGene(i, staffPool.get(random.nextInt(staffPool.size())));
            }
        }
    }

    private RosterChromosome bestOf(List<RosterChromosome> population) {
        return population.stream()
                .max(Comparator.comparingDouble(RosterChromosome::getFitness))
                .orElseThrow();
    }

    /** Converged once the best fitness has improved by less than convergenceThreshold over the last convergenceWindow generations. */
    private boolean hasConverged(List<Double> bestFitnessHistory) {
        int window = params.convergenceWindow();
        if (bestFitnessHistory.size() <= window) {
            return false;
        }
        double past = bestFitnessHistory.get(bestFitnessHistory.size() - 1 - window);
        double current = bestFitnessHistory.get(bestFitnessHistory.size() - 1);
        return Math.abs(current - past) < params.convergenceThreshold();
    }
}
