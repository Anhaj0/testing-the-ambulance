package com.ambulance.dispatch_system.optimization.model;

import com.ambulance.dispatch_system.common.entity.Staff;

import java.util.Arrays;

/**
 * A candidate full-week roster: one Staff assignment per seat in the
 * problem's expandedSlots list. This is the Genetic Algorithm's
 * chromosome, and is also the representation produced by the Greedy
 * baseline, so both algorithms can be scored with the same
 * RosterFitnessEvaluator for a fair comparison.
 */
public class RosterChromosome {

    private final Staff[] genes;
    private double fitness = Double.NaN;

    public RosterChromosome(Staff[] genes) {
        this.genes = genes;
    }

    /** Creates an all-null chromosome of the given length, ready to be filled in gene by gene. */
    public static RosterChromosome empty(int size) {
        return new RosterChromosome(new Staff[size]);
    }

    public Staff[] getGenes() {
        return genes;
    }

    public int size() {
        return genes.length;
    }

    public Staff getGene(int index) {
        return genes[index];
    }

    public void setGene(int index, Staff staff) {
        genes[index] = staff;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    /** Copies the gene array (Staff references are shared, not cloned) so GA operators never mutate a parent. */
    public RosterChromosome copy() {
        RosterChromosome clone = new RosterChromosome(Arrays.copyOf(genes, genes.length));
        clone.fitness = this.fitness;
        return clone;
    }
}
