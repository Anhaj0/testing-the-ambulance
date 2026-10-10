package com.ambulance.dispatch_system.optimization.benchmark;

import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.common.entity.enums.Certification;
import com.ambulance.dispatch_system.common.entity.enums.StaffRole;
import com.ambulance.dispatch_system.optimization.fitness.FitnessResult;
import com.ambulance.dispatch_system.optimization.fitness.FitnessWeights;
import com.ambulance.dispatch_system.optimization.ga.GAParameters;
import com.ambulance.dispatch_system.optimization.ga.GeneticAlgorithmScheduler;
import com.ambulance.dispatch_system.optimization.greedy.GreedyRosterScheduler;
import com.ambulance.dispatch_system.optimization.model.SchedulingProblem;
import com.ambulance.dispatch_system.optimization.model.SchedulingResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Benchmarking suite comparing {@link GeneticAlgorithmScheduler} against
 * {@link GreedyRosterScheduler} - execution time, memory consumption and
 * solution quality across a range of roster sizes - so the experimental
 * evaluation chapter can cite real measured numbers instead of test-suite
 * timings.
 *
 * <p>N is the number of seats to fill (the expanded-slot count a
 * RosterChromosome is sized to); the synthetic staff pool for each N is
 * sized at N/5, roughly half of whom hold {@code ECG_CERTIFIED} against
 * shift slots that require it about half the time, so both algorithms have
 * real understaffing/overtime/rest-violation pressure to resolve rather
 * than a trivial fully-qualified instance.
 *
 * <p>Unlike a single fitness evaluation, one GA run costs
 * O(G * P * n log n) (see {@link GeneticAlgorithmScheduler}'s class
 * Javadoc), so timed runs are kept to 3 rather than the 10 used for
 * cheaper per-call benchmarks elsewhere (see TriageBenchmarkTest) - enough
 * for a median without needlessly multiplying a report figure's runtime.
 *
 * <p>JVM memory profiling via Runtime is inherently approximate. -Xms and
 * -Xmx flags would help stabilize GC behavior for rigorous memory testing.
 */
public class SchedulingBenchmarkTest {

    private static final LocalDate WEEK_STARTING = LocalDate.of(2024, 1, 1); // a Monday
    private static final FitnessWeights WEIGHTS = FitnessWeights.defaults();
    private static final GAParameters GA_PARAMS = GAParameters.defaults();

    private static final int WARMUP_ITERATIONS = 1;
    private static final int TIMED_RUNS = 3;
    private static final int[] SEAT_COUNTS = {10, 50, 100, 500, 1000};
    private static final String OUTPUT_CSV = "docs/scheduling-benchmark-results.csv";

    // Disabled by default: N scales to 1000 seats and each GA run is O(G*P*n log n),
    // so the full sweep takes a while. Remove @Disabled to regenerate the report's
    // scheduling-benchmark-results.csv locally on demand.
    @Test
    @Disabled("Long-running benchmark; run manually to regenerate report data")
    public void runBenchmarks() throws IOException {
        System.out.println("Starting Scheduling Benchmarks...");

        Path outputPath = Path.of(OUTPUT_CSV);
        Files.createDirectories(outputPath.getParent());

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            writer.println("algorithm,N,staffPoolSize,avg_time_ms,median_time_ms,stddev_time_ms,memory_delta_kb,"
                    + "generations_run,fitness,understaffed,overtime_hours,rest_violations");

            for (int n : SEAT_COUNTS) {
                System.out.println("Benchmarking N=" + n);
                SchedulingProblem problem = generateProblem(n);

                benchmarkGreedy(problem, n, writer);
                benchmarkGA(problem, n, writer);
            }
        }

        System.out.println("Benchmarks complete. Results saved to " + OUTPUT_CSV);
    }

    private void benchmarkGA(SchedulingProblem problem, int n, PrintWriter writer) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            new GeneticAlgorithmScheduler(problem, GA_PARAMS, WEIGHTS, new Random(1000 + i)).run();
        }

        double[] times = new double[TIMED_RUNS];
        SchedulingResult[] runs = new SchedulingResult[TIMED_RUNS];
        for (int i = 0; i < TIMED_RUNS; i++) {
            long start = System.nanoTime();
            runs[i] = new GeneticAlgorithmScheduler(problem, GA_PARAMS, WEIGHTS, new Random(2000 + i)).run();
            times[i] = (System.nanoTime() - start) / 1_000_000.0;
        }

        double memKb = measureMemory(() ->
                new GeneticAlgorithmScheduler(problem, GA_PARAMS, WEIGHTS, new Random(3000)).run());

        logAndWrite("Genetic Algorithm", n, problem.staffPool().size(), times, memKb, runs, writer);
    }

    private void benchmarkGreedy(SchedulingProblem problem, int n, PrintWriter writer) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            new GreedyRosterScheduler(problem, WEIGHTS).run();
        }

        double[] times = new double[TIMED_RUNS];
        SchedulingResult[] runs = new SchedulingResult[TIMED_RUNS];
        for (int i = 0; i < TIMED_RUNS; i++) {
            long start = System.nanoTime();
            runs[i] = new GreedyRosterScheduler(problem, WEIGHTS).run();
            times[i] = (System.nanoTime() - start) / 1_000_000.0;
        }

        double memKb = measureMemory(() -> new GreedyRosterScheduler(problem, WEIGHTS).run());

        logAndWrite("Greedy", n, problem.staffPool().size(), times, memKb, runs, writer);
    }

    /** Builds a synthetic scheduling instance with seatCount seats and a staffCount = seatCount/5 pool. */
    private SchedulingProblem generateProblem(int seatCount) {
        int staffCount = Math.max(1, seatCount / 5);

        List<Staff> staffPool = new ArrayList<>(staffCount);
        for (int i = 0; i < staffCount; i++) {
            Staff staff = new Staff();
            staff.setName("Staff-" + i);
            staff.setRole(StaffRole.PARAMEDIC);
            staff.setCertifications(i % 2 == 0 ? Set.of(Certification.ECG_CERTIFIED) : Set.of());
            // A fifth of staff have a reduced weekly cap so overtime pressure is real, not just theoretical.
            staff.setMaxWeeklyHours(i % 5 == 0 ? 30 : 40);
            staffPool.add(staff);
        }

        DayOfWeek[] days = DayOfWeek.values();
        List<ShiftSlot> slots = new ArrayList<>(seatCount);
        for (int i = 0; i < seatCount; i++) {
            ShiftSlot slot = new ShiftSlot();
            slot.setDayOfWeek(days[i % days.length]);
            slot.setStartTime(LocalTime.of(8, 0));
            slot.setEndTime(LocalTime.of(16, 0));
            slot.setRequiredCertification(i % 2 == 0 ? Certification.ECG_CERTIFIED : null);
            slot.setRequiredStaffCount(1);
            slots.add(slot);
        }
        // Dataset is deterministic by construction (fixed alternating pattern), so every
        // algorithm/run in the sweep is benchmarked against the identical instance for a given N.

        return new SchedulingProblem(slots, staffPool, WEEK_STARTING);
    }

    private double measureMemory(Supplier<Object> task) {
        double[] deltas = new double[5];
        for (int i = 0; i < 5; i++) {
            forceGC();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            Object retained = task.get();

            forceGC();
            long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            deltas[i] = Math.max(0, (memAfter - memBefore) / 1024.0);

            if (retained != null) {
                retained.hashCode(); // prevent optimization removal
            }
        }
        Arrays.sort(deltas);
        return deltas[2]; // Median
    }

    private void forceGC() {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void logAndWrite(String algorithm, int n, int staffPoolSize, double[] times, double memKb,
                              SchedulingResult[] runs, PrintWriter writer) {
        Arrays.sort(times);
        double median = (times[times.length / 2] + times[(times.length - 1) / 2]) / 2.0;

        double sum = 0;
        for (double t : times) sum += t;
        double avg = sum / times.length;

        double sqSum = 0;
        for (double t : times) sqSum += (t - avg) * (t - avg);
        double stddev = Math.sqrt(sqSum / times.length);

        // Solution-quality columns use the median-fitness run of the batch, consistent with the timing median.
        SchedulingResult representative = medianByFitness(runs);
        FitnessResult fitness = representative.fitnessResult();

        writer.printf("%s,%d,%d,%.3f,%.3f,%.3f,%.2f,%d,%.3f,%d,%.3f,%d%n",
                algorithm, n, staffPoolSize, avg, median, stddev, memKb,
                representative.generationsRun(), fitness.fitness(), fitness.understaffedViolations(),
                fitness.overtimeHours(), fitness.restViolations());

        System.out.printf("%-20s | N=%-5d | median=%.2fms | mem=%.2fKB | fitness=%.2f%n",
                algorithm, n, median, memKb, fitness.fitness());
    }

    private SchedulingResult medianByFitness(SchedulingResult[] runs) {
        SchedulingResult[] sorted = runs.clone();
        Arrays.sort(sorted, java.util.Comparator.comparingDouble(r -> r.fitnessResult().fitness()));
        return sorted[sorted.length / 2];
    }
}
