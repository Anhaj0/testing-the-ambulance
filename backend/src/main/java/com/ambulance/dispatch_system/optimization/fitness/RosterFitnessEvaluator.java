package com.ambulance.dispatch_system.optimization.fitness;

import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.optimization.model.RosterChromosome;
import com.ambulance.dispatch_system.optimization.model.SchedulingProblem;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scores a RosterChromosome against a SchedulingProblem, penalising
 * (see CLAUDE.md - Genetic Algorithm requirements):
 * <ul>
 *   <li>understaffed shifts - an empty seat or one filled by a staff
 *       member missing the required certification;</li>
 *   <li>overtime - hours worked beyond a staff member's maxWeeklyHours;</li>
 *   <li>insufficient rest - back-to-back shifts closer together than
 *       {@link FitnessWeights#minRestHours()};</li>
 *   <li>unfair distribution - spread of hours worked across the whole
 *       staff pool, measured as a population standard deviation.</li>
 * </ul>
 *
 * <p><b>Complexity:</b> O(n) to walk the n assigned seats and accumulate
 * per-staff hours, plus O(n log n) worst case to sort every staff
 * member's own shifts by start time when checking rest periods (the sum
 * of each staff member's shift count times its log is bounded by
 * n log n when one staff member holds all n seats). Overall O(n log n)
 * per evaluation.
 */
public class RosterFitnessEvaluator {

    private final FitnessWeights weights;

    public RosterFitnessEvaluator(FitnessWeights weights) {
        this.weights = weights;
    }

    public FitnessResult evaluate(RosterChromosome chromosome, SchedulingProblem problem) {
        List<ShiftSlot> slots = problem.expandedSlots();

        int understaffed = 0;
        Map<Staff, Double> hoursWorked = new HashMap<>();
        Map<Staff, List<ShiftSlot>> shiftsByStaff = new HashMap<>();

        for (int i = 0; i < slots.size(); i++) {
            ShiftSlot slot = slots.get(i);
            Staff staff = chromosome.getGene(i);

            if (staff == null || !isQualified(staff, slot)) {
                understaffed++;
                continue; // an unfilled/unqualified seat does not add to anyone's worked hours
            }

            hoursWorked.merge(staff, slot.getDurationHours(), Double::sum);
            shiftsByStaff.computeIfAbsent(staff, s -> new ArrayList<>()).add(slot);
        }

        double overtimeHours = totalOvertimeHours(hoursWorked);
        int restViolations = countRestViolations(shiftsByStaff, problem);
        double fairnessStdDev = fairnessStdDev(problem.staffPool(), hoursWorked);

        double totalPenalty =
                understaffed * weights.understaffedPenalty()
                        + overtimeHours * weights.overtimePenaltyPerHour()
                        + restViolations * weights.restViolationPenalty()
                        + fairnessStdDev * weights.fairnessWeight();

        FitnessResult result = new FitnessResult(
                understaffed, overtimeHours, restViolations, fairnessStdDev, totalPenalty, -totalPenalty);
        chromosome.setFitness(result.fitness());
        return result;
    }

    private boolean isQualified(Staff staff, ShiftSlot slot) {
        return slot.getRequiredCertification() == null
                || staff.getCertifications().contains(slot.getRequiredCertification());
    }

    private double totalOvertimeHours(Map<Staff, Double> hoursWorked) {
        double total = 0;
        for (Map.Entry<Staff, Double> entry : hoursWorked.entrySet()) {
            double excess = entry.getValue() - entry.getKey().getMaxWeeklyHours();
            if (excess > 0) {
                total += excess;
            }
        }
        return total;
    }

    private int countRestViolations(Map<Staff, List<ShiftSlot>> shiftsByStaff, SchedulingProblem problem) {
        int violations = 0;
        for (List<ShiftSlot> assigned : shiftsByStaff.values()) {
            assigned.sort(Comparator.comparing(problem::startOf));
            for (int i = 1; i < assigned.size(); i++) {
                LocalDateTime previousEnd = problem.endOf(assigned.get(i - 1));
                LocalDateTime nextStart = problem.startOf(assigned.get(i));
                double restHours = ChronoUnit.MINUTES.between(previousEnd, nextStart) / 60.0;
                if (restHours < weights.minRestHours()) {
                    violations++;
                }
            }
        }
        return violations;
    }

    /** Population standard deviation of hours worked across the whole staff pool (unassigned staff count as 0h). */
    private double fairnessStdDev(List<Staff> staffPool, Map<Staff, Double> hoursWorked) {
        if (staffPool.isEmpty()) {
            return 0;
        }
        double mean = staffPool.stream().mapToDouble(s -> hoursWorked.getOrDefault(s, 0.0)).average().orElse(0);
        double variance = staffPool.stream()
                .mapToDouble(s -> Math.pow(hoursWorked.getOrDefault(s, 0.0) - mean, 2))
                .average().orElse(0);
        return Math.sqrt(variance);
    }
}
