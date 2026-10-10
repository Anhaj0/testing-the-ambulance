package com.ambulance.dispatch_system.optimization.fitness;

import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.common.entity.enums.Certification;
import com.ambulance.dispatch_system.common.entity.enums.StaffRole;
import com.ambulance.dispatch_system.optimization.model.RosterChromosome;
import com.ambulance.dispatch_system.optimization.model.SchedulingProblem;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the shift-scheduling fitness function. Each test
 * isolates a single penalty component so a broken weight or condition
 * points straight to the offending method.
 */
class RosterFitnessEvaluatorTest {

    // 2024-01-01 is a Monday - used as a fixed, deterministic scheduling week start.
    private static final LocalDate WEEK_STARTING = LocalDate.of(2024, 1, 1);
    private static final FitnessWeights WEIGHTS = FitnessWeights.defaults();

    private Staff staff(String name, int maxWeeklyHours, Certification... certifications) {
        Staff s = new Staff();
        s.setName(name);
        s.setRole(StaffRole.PARAMEDIC);
        s.setMaxWeeklyHours(maxWeeklyHours);
        s.setCertifications(Set.of(certifications));
        return s;
    }

    private ShiftSlot slot(DayOfWeek day, LocalTime start, LocalTime end, Certification required) {
        ShiftSlot slot = new ShiftSlot();
        slot.setDayOfWeek(day);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setRequiredCertification(required);
        slot.setRequiredStaffCount(1);
        return slot;
    }

    @Test
    void qualifiedAssignmentIncursNoCoverageOvertimeOrRestPenalty() {
        Staff alice = staff("Alice", 40, Certification.ECG_CERTIFIED);
        Staff bob = staff("Bob", 40);
        ShiftSlot morningShift = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), Certification.ECG_CERTIFIED);

        SchedulingProblem problem = new SchedulingProblem(List.of(morningShift), List.of(alice, bob), WEEK_STARTING);
        RosterChromosome chromosome = new RosterChromosome(new Staff[]{alice});

        FitnessResult result = new RosterFitnessEvaluator(WEIGHTS).evaluate(chromosome, problem);

        assertEquals(0, result.understaffedViolations());
        assertEquals(0.0, result.overtimeHours());
        assertEquals(0, result.restViolations());
        // Alice worked 8h, Bob 0h -> mean 4h, stddev 4h - the only non-zero penalty component.
        assertEquals(4.0, result.fairnessStdDevHours(), 1e-9);
        assertEquals(-result.totalPenalty(), result.fitness(), 1e-9);
    }

    @Test
    void unqualifiedStaffCountsAsUnderstaffed() {
        Staff bob = staff("Bob", 40); // no certifications
        ShiftSlot morningShift = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), Certification.ECG_CERTIFIED);

        SchedulingProblem problem = new SchedulingProblem(List.of(morningShift), List.of(bob), WEEK_STARTING);
        RosterChromosome chromosome = new RosterChromosome(new Staff[]{bob});

        FitnessResult result = new RosterFitnessEvaluator(WEIGHTS).evaluate(chromosome, problem);

        assertEquals(1, result.understaffedViolations());
        assertEquals(WEIGHTS.understaffedPenalty(), result.totalPenalty(), 1e-9);
    }

    @Test
    void emptySeatCountsAsUnderstaffed() {
        ShiftSlot morningShift = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), null);
        Staff onlyStaff = staff("Alice", 40);

        SchedulingProblem problem = new SchedulingProblem(List.of(morningShift), List.of(onlyStaff), WEEK_STARTING);
        RosterChromosome chromosome = new RosterChromosome(new Staff[]{null});

        FitnessResult result = new RosterFitnessEvaluator(WEIGHTS).evaluate(chromosome, problem);

        assertEquals(1, result.understaffedViolations());
    }

    @Test
    void hoursBeyondMaxWeeklyHoursArePenalisedAsOvertime() {
        Staff staff = staff("Alice", 10); // capacity of only 10h
        ShiftSlot longShift = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(20, 0), null); // 12h shift

        SchedulingProblem problem = new SchedulingProblem(List.of(longShift), List.of(staff), WEEK_STARTING);
        RosterChromosome chromosome = new RosterChromosome(new Staff[]{staff});

        FitnessResult result = new RosterFitnessEvaluator(WEIGHTS).evaluate(chromosome, problem);

        assertEquals(2.0, result.overtimeHours(), 1e-9); // 12h worked - 10h max
    }

    @Test
    void backToBackShiftsWithoutEnoughRestArePenalised() {
        Staff staff = staff("Alice", 80);
        ShiftSlot first = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), null);
        ShiftSlot second = slot(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(22, 0), null); // only 2h after `first` ends

        SchedulingProblem problem = new SchedulingProblem(List.of(first, second), List.of(staff), WEEK_STARTING);
        RosterChromosome chromosome = new RosterChromosome(new Staff[]{staff, staff});

        FitnessResult result = new RosterFitnessEvaluator(WEIGHTS).evaluate(chromosome, problem);

        assertEquals(1, result.restViolations());
    }

    @Test
    void restOfExactlyTheMinimumIsNotAViolation() {
        Staff staff = staff("Alice", 80);
        ShiftSlot first = slot(DayOfWeek.MONDAY, LocalTime.of(0, 0), LocalTime.of(8, 0), null);
        ShiftSlot second = slot(DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(23, 0), null); // exactly 8h after `first` ends

        SchedulingProblem problem = new SchedulingProblem(List.of(first, second), List.of(staff), WEEK_STARTING);
        RosterChromosome chromosome = new RosterChromosome(new Staff[]{staff, staff});

        FitnessResult result = new RosterFitnessEvaluator(WEIGHTS).evaluate(chromosome, problem);

        assertEquals(0, result.restViolations());
    }

    @Test
    void fairnessPenaltyGrowsWithUnevenDistribution() {
        Staff overworked = staff("Alice", 100);
        Staff idle = staff("Bob", 100);
        ShiftSlot shiftA = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), null);
        ShiftSlot shiftB = slot(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), null);

        SchedulingProblem problem = new SchedulingProblem(List.of(shiftA, shiftB), List.of(overworked, idle), WEEK_STARTING);

        RosterChromosome unevenRoster = new RosterChromosome(new Staff[]{overworked, overworked}); // Alice: 16h, Bob: 0h
        RosterChromosome evenRoster = new RosterChromosome(new Staff[]{overworked, idle}); // Alice: 8h, Bob: 8h

        RosterFitnessEvaluator evaluator = new RosterFitnessEvaluator(WEIGHTS);
        FitnessResult uneven = evaluator.evaluate(unevenRoster, problem);
        FitnessResult even = evaluator.evaluate(evenRoster, problem);

        assertTrue(uneven.fairnessStdDevHours() > even.fairnessStdDevHours());
        assertTrue(uneven.fitness() < even.fitness()); // more unfair -> worse (lower) fitness
    }
}
