package com.ambulance.dispatch_system.optimization.greedy;

import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.common.entity.enums.Certification;
import com.ambulance.dispatch_system.common.entity.enums.StaffRole;
import com.ambulance.dispatch_system.optimization.fitness.FitnessWeights;
import com.ambulance.dispatch_system.optimization.model.SchedulingProblem;
import com.ambulance.dispatch_system.optimization.model.SchedulingResult;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreedyRosterSchedulerTest {

    private static final LocalDate WEEK_STARTING = LocalDate.of(2024, 1, 1); // a Monday
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
    void fillsEverySeatWhenEnoughQualifiedStaffExist() {
        Staff alice = staff("Alice", 40, Certification.ECG_CERTIFIED);
        Staff bob = staff("Bob", 40, Certification.ECG_CERTIFIED);
        ShiftSlot morning = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), Certification.ECG_CERTIFIED);
        ShiftSlot afternoon = slot(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), Certification.ECG_CERTIFIED);

        SchedulingProblem problem = new SchedulingProblem(List.of(morning, afternoon), List.of(alice, bob), WEEK_STARTING);
        SchedulingResult result = new GreedyRosterScheduler(problem, WEIGHTS).run();

        assertEquals(0, result.fitnessResult().understaffedViolations());
        for (Staff assigned : result.bestChromosome().getGenes()) {
            assertNotNull(assigned);
        }
    }

    @Test
    void leavesSeatEmptyWhenNoStaffIsQualified() {
        Staff bob = staff("Bob", 40); // not ECG certified
        ShiftSlot morning = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), Certification.ECG_CERTIFIED);

        SchedulingProblem problem = new SchedulingProblem(List.of(morning), List.of(bob), WEEK_STARTING);
        SchedulingResult result = new GreedyRosterScheduler(problem, WEIGHTS).run();

        assertNull(result.bestChromosome().getGene(0));
        assertEquals(1, result.fitnessResult().understaffedViolations());
    }

    @Test
    void prefersTheLeastLoadedQualifiedStaffMember() {
        Staff alice = staff("Alice", 80);
        Staff bob = staff("Bob", 80);
        ShiftSlot shiftA = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), null);
        ShiftSlot shiftB = slot(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), null);
        ShiftSlot shiftC = slot(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), null);

        // Both staff start at 0h; the greedy rule should alternate them to keep hours balanced.
        SchedulingProblem problem = new SchedulingProblem(List.of(shiftA, shiftB, shiftC), List.of(alice, bob), WEEK_STARTING);
        SchedulingResult result = new GreedyRosterScheduler(problem, WEIGHTS).run();

        Staff[] genes = result.bestChromosome().getGenes();
        assertTrue(genes[0] != genes[1], "second shift should go to the staff member left idle by the first");
    }

    @Test
    void fallsBackToOvertimeRatherThanLeavingASeatEmpty() {
        // Only one qualified staff member, already at capacity - greedy must still fill the seat.
        Staff onlyQualified = staff("Alice", 8, Certification.ECG_CERTIFIED);
        ShiftSlot firstShift = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), Certification.ECG_CERTIFIED); // fills 8h capacity
        ShiftSlot secondShift = slot(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), Certification.ECG_CERTIFIED); // pushes into overtime

        SchedulingProblem problem = new SchedulingProblem(List.of(firstShift, secondShift), List.of(onlyQualified), WEEK_STARTING);
        SchedulingResult result = new GreedyRosterScheduler(problem, WEIGHTS).run();

        assertNotNull(result.bestChromosome().getGene(1));
        assertTrue(result.fitnessResult().overtimeHours() > 0);
        assertEquals(0, result.fitnessResult().understaffedViolations());
    }

    @Test
    void reportsZeroGenerationsAndNonNegativeExecutionTime() {
        Staff alice = staff("Alice", 40);
        ShiftSlot shift = slot(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0), null);

        SchedulingProblem problem = new SchedulingProblem(List.of(shift), List.of(alice), WEEK_STARTING);
        SchedulingResult result = new GreedyRosterScheduler(problem, WEIGHTS).run();

        assertEquals(0, result.generationsRun());
        assertTrue(result.executionTimeMillis() >= 0);
        assertEquals("Greedy", result.algorithmName());
    }
}
