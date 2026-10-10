package com.ambulance.dispatch_system.optimization.ga;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneticAlgorithmSchedulerTest {

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

    /** A week of 5 single-staff day shifts, with only 2 of 4 staff ECG-certified, so the GA has real work to do. */
    private SchedulingProblem sampleProblem() {
        Staff alice = staff("Alice", 40, Certification.ECG_CERTIFIED);
        Staff bob = staff("Bob", 40, Certification.ECG_CERTIFIED);
        Staff carol = staff("Carol", 40);
        Staff dave = staff("Dave", 40);

        List<ShiftSlot> shiftSlots = new ArrayList<>();
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
        for (DayOfWeek day : days) {
            shiftSlots.add(slot(day, LocalTime.of(8, 0), LocalTime.of(16, 0), Certification.ECG_CERTIFIED));
        }

        return new SchedulingProblem(shiftSlots, List.of(alice, bob, carol, dave), WEEK_STARTING);
    }

    @Test
    void bestChromosomeCoversEverySlot() {
        SchedulingProblem problem = sampleProblem();
        GAParameters params = GAParameters.defaults();

        SchedulingResult result = new GeneticAlgorithmScheduler(problem, params, WEIGHTS, new Random(42)).run();

        assertEquals(problem.expandedSlots().size(), result.bestChromosome().size());
    }

    @Test
    void elitismMeansBestFitnessNeverRegressesAcrossGenerations() {
        SchedulingProblem problem = sampleProblem();
        GAParameters params = GAParameters.defaults();

        SchedulingResult result = new GeneticAlgorithmScheduler(problem, params, WEIGHTS, new Random(7)).run();

        List<Double> history = result.bestFitnessHistory();
        for (int i = 1; i < history.size(); i++) {
            assertTrue(history.get(i) >= history.get(i - 1),
                    "best fitness should be monotonically non-decreasing thanks to elitism");
        }
    }

    @Test
    void convergesAndStopsBeforeTheGenerationCap() {
        SchedulingProblem problem = sampleProblem();
        // A generous convergence window relative to a large generation cap should trigger early stopping
        // on a problem this small, since the population saturates on a near-optimal roster quickly.
        GAParameters params = new GAParameters(30, 500, 0.8, 0.05, 2, 5, 0.01, 15);

        SchedulingResult result = new GeneticAlgorithmScheduler(problem, params, WEIGHTS, new Random(1)).run();

        assertTrue(result.generationsRun() < params.maxGenerations());
    }

    @Test
    void findsAFullyQualifiedZeroUnderstaffingRosterOnThisSmallProblem() {
        SchedulingProblem problem = sampleProblem(); // exactly 2 ECG-certified staff, 5 seats each needing one
        GAParameters params = GAParameters.defaults();

        SchedulingResult result = new GeneticAlgorithmScheduler(problem, params, WEIGHTS, new Random(123)).run();

        assertEquals(0, result.fitnessResult().understaffedViolations());
    }

    @Test
    void sameSeedProducesReproducibleResults() {
        SchedulingProblem problem = sampleProblem();
        GAParameters params = GAParameters.defaults();

        SchedulingResult first = new GeneticAlgorithmScheduler(problem, params, WEIGHTS, new Random(99)).run();
        SchedulingResult second = new GeneticAlgorithmScheduler(problem, params, WEIGHTS, new Random(99)).run();

        assertEquals(first.fitnessResult().fitness(), second.fitnessResult().fitness(), 1e-9);
        assertEquals(first.generationsRun(), second.generationsRun());
    }
}
