package com.ambulance.dispatch_system.optimization.greedy;

import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.optimization.fitness.RosterFitnessEvaluator;
import com.ambulance.dispatch_system.optimization.fitness.FitnessResult;
import com.ambulance.dispatch_system.optimization.fitness.FitnessWeights;
import com.ambulance.dispatch_system.optimization.model.RosterChromosome;
import com.ambulance.dispatch_system.optimization.model.SchedulingProblem;
import com.ambulance.dispatch_system.optimization.model.SchedulingResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Greedy baseline for shift scheduling, used purely to generate
 * comparison data against the Genetic Algorithm - solution quality,
 * constraint violations and execution time - for the evaluation
 * chapter (see CLAUDE.md - Greedy Baseline requirements).
 *
 * <p>Each seat is filled once, in the order it appears, by the
 * qualified staff member with the fewest hours worked so far who can
 * still take the shift without going into overtime; if no qualified
 * staff member has capacity left, the least-loaded qualified staff
 * member is assigned anyway (recording an overtime violation) rather
 * than leaving the seat empty. There is no backtracking: once a seat is
 * filled the choice is never revisited, so a locally "fair" run of
 * choices can still leave a later seat understaffed even when a better
 * full assignment exists.
 *
 * <p><b>Complexity:</b> O(n * m) - a linear scan of the m staff members
 * for each of the n seats.
 */
public class GreedyRosterScheduler {

    private final SchedulingProblem problem;
    private final RosterFitnessEvaluator evaluator;

    public GreedyRosterScheduler(SchedulingProblem problem, FitnessWeights weights) {
        this.problem = problem;
        this.evaluator = new RosterFitnessEvaluator(weights);
    }

    public SchedulingResult run() {
        long startTime = System.nanoTime();

        List<ShiftSlot> slots = problem.expandedSlots();
        List<Staff> staffPool = problem.staffPool();

        Map<Staff, Double> hoursWorked = new HashMap<>();
        staffPool.forEach(s -> hoursWorked.put(s, 0.0));

        RosterChromosome chromosome = RosterChromosome.empty(slots.size());

        for (int i = 0; i < slots.size(); i++) {
            ShiftSlot slot = slots.get(i);
            Staff choice = pickBestAvailableStaff(slot, staffPool, hoursWorked);
            chromosome.setGene(i, choice);
            if (choice != null) {
                hoursWorked.merge(choice, slot.getDurationHours(), Double::sum);
            }
        }

        long elapsedMillis = (System.nanoTime() - startTime) / 1_000_000;
        FitnessResult fitnessResult = evaluator.evaluate(chromosome, problem);

        // No generations for a single-pass greedy run; the history has one point so it plots alongside the GA's curve.
        return new SchedulingResult(
                "Greedy", chromosome, fitnessResult, elapsedMillis, 0, List.of(chromosome.getFitness()));
    }

    /**
     * Prefers the qualified staff member with the fewest hours worked so far who has capacity
     * left under their maxWeeklyHours; falls back to the least-loaded qualified staff member
     * (accepting an overtime violation) if nobody qualified has capacity; returns null if nobody
     * is qualified at all (the seat is left understaffed).
     */
    private Staff pickBestAvailableStaff(ShiftSlot slot, List<Staff> staffPool, Map<Staff, Double> hoursWorked) {
        Staff bestWithinCapacity = null;
        double lowestWithinCapacity = Double.MAX_VALUE;
        Staff bestOverCapacity = null;
        double lowestOverCapacity = Double.MAX_VALUE;

        for (Staff staff : staffPool) {
            if (!isQualified(staff, slot)) {
                continue;
            }
            double hoursSoFar = hoursWorked.get(staff);
            boolean hasCapacity = hoursSoFar + slot.getDurationHours() <= staff.getMaxWeeklyHours();

            if (hasCapacity && hoursSoFar < lowestWithinCapacity) {
                lowestWithinCapacity = hoursSoFar;
                bestWithinCapacity = staff;
            } else if (!hasCapacity && hoursSoFar < lowestOverCapacity) {
                lowestOverCapacity = hoursSoFar;
                bestOverCapacity = staff;
            }
        }

        return bestWithinCapacity != null ? bestWithinCapacity : bestOverCapacity;
    }

    private boolean isQualified(Staff staff, ShiftSlot slot) {
        return slot.getRequiredCertification() == null
                || staff.getCertifications().contains(slot.getRequiredCertification());
    }
}
