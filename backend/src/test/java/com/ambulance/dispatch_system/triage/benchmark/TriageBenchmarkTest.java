package com.ambulance.dispatch_system.triage.benchmark;

import com.ambulance.dispatch_system.common.entity.TriageAssessment;
import com.ambulance.dispatch_system.common.entity.enums.ConsciousnessLevel;
import com.ambulance.dispatch_system.common.entity.enums.TriageCategory;
import com.ambulance.dispatch_system.triage.dto.TriageRequestDTO;
import com.ambulance.dispatch_system.triage.service.impl.algorithms.MTSDecisionTree;
import com.ambulance.dispatch_system.triage.service.impl.algorithms.WeightedScoringStrategy;
import com.ambulance.dispatch_system.triage.util.PriorityDispatchQueue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Benchmarking suite to compare algorithms.
 * Note: N defines the size of the underlying data structure:
 * - KNNClassifier: N = size of the historical dataset.
 * - PriorityDispatchQueue: N = initial number of elements in the queue.
 * - MTSDecisionTree: O(1) stateless evaluation, N is kept for alignment.
 * 
 * We measure the time taken to evaluate a FIXED number of Q incoming requests (Q = 1000)
 * against these N-sized structures to correctly isolate the O(N*D) and O(log N) per-query costs.
 * 
 * JVM memory profiling via Runtime is inherently approximate. -Xms and -Xmx flags 
 * would help stabilize GC behavior for rigorous memory testing.
 */
public class TriageBenchmarkTest {

    private final MTSDecisionTree mtsDecisionTree = new MTSDecisionTree();
    private final WeightedScoringStrategy scoringStrategy = new WeightedScoringStrategy();
    private final KNNClassifier knnClassifier = new KNNClassifier();

    private static final int WARMUP_ITERATIONS = 3;
    private static final int TIMED_RUNS = 10;
    private static final int[] N_VALUES = {100, 1000, 10000, 100000};
    private static final int QUERIES = 1000;

    // Disabled by default: N scales to 100,000 with 1,000 queries per N against the O(N)
    // KNN baseline, which takes several minutes. Remove @Disabled to regenerate the
    // report's benchmark-results.csv / complexity-validation.csv locally on demand.
    @Test
    @Disabled("Long-running benchmark; run manually to regenerate report data")
    public void runBenchmarks() throws IOException {
        System.out.println("Starting Benchmarks...");
        
        List<BenchmarkResult> results = new ArrayList<>();

        try (PrintWriter writer = new PrintWriter(new FileWriter("benchmark-results.csv"))) {
            writer.println("algorithm,N,avg_time_ms,median_time_ms,stddev_time_ms,memory_delta_kb,resident_footprint_kb");

            for (int n : N_VALUES) {
                System.out.println("Benchmarking N=" + n);
                List<TriageRequestDTO> queries = generateRequests(QUERIES);

                results.add(benchmarkMTS(queries, n, writer));
                results.add(benchmarkWeightedHeap(queries, n, writer));
                results.add(benchmarkKNN(queries, n, writer));
            }
        }
        
        System.out.println("Benchmarks complete. Results saved to benchmark-results.csv");
        generateComplexityValidation(results);
    }
    
    private void generateComplexityValidation(List<BenchmarkResult> results) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("complexity-validation.csv"))) {
            writer.println("algorithm,N_from,N_to,empirical_ratio,expected_ratio_O1,expected_ratio_OlogN,expected_ratio_ON");
            System.out.println("\nComplexity Validation Check");
            System.out.printf("%-20s | %-8s | %-8s | %-15s | %-10s | %-10s | %-10s%n", 
                              "Algorithm", "N_from", "N_to", "Empirical", "O(1)", "O(log N)", "O(N)");
            System.out.println("-".repeat(95));
            
            for (int i = 0; i < results.size(); i++) {
                BenchmarkResult current = results.get(i);
                BenchmarkResult prev = null;
                for (int j = i - 1; j >= 0; j--) {
                    if (results.get(j).algo.equals(current.algo)) {
                        prev = results.get(j);
                        break;
                    }
                }
                
                if (prev != null) {
                    double empirical = current.medianTimeMs / prev.medianTimeMs;
                    double expO1 = 1.0;
                    double expLogN = (prev.n > 1) ? (Math.log(current.n) / Math.log(prev.n)) : 1.0;
                    double expON = (double) current.n / prev.n;
                    
                    writer.printf("%s,%d,%d,%.3f,%.3f,%.3f,%.3f%n", 
                            current.algo, prev.n, current.n, empirical, expO1, expLogN, expON);
                    System.out.printf("%-20s | %-8d | %-8d | %-15.3f | %-10.3f | %-10.3f | %-10.3f%n",
                            current.algo, prev.n, current.n, empirical, expO1, expLogN, expON);
                }
            }
        }
        System.out.println("Complexity validation saved to complexity-validation.csv\n");
    }

    private BenchmarkResult benchmarkMTS(List<TriageRequestDTO> queries, int n, PrintWriter writer) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            for (TriageRequestDTO req : queries) mtsDecisionTree.evaluate(req);
        }

        double[] times = new double[TIMED_RUNS];
        for (int i = 0; i < TIMED_RUNS; i++) {
            long start = System.nanoTime();
            for (TriageRequestDTO req : queries) mtsDecisionTree.evaluate(req);
            times[i] = (System.nanoTime() - start) / 1_000_000.0;
        }

        double memKb = measureMemory(() -> {
            List<TriageCategory> res = new ArrayList<>(QUERIES);
            for (TriageRequestDTO req : queries) res.add(mtsDecisionTree.evaluate(req));
            return res;
        });

        return logAndCreateResult("MTSDecisionTree", n, times, memKb, "N/A", writer);
    }

    private BenchmarkResult benchmarkWeightedHeap(List<TriageRequestDTO> queries, int n, PrintWriter writer) {
        List<TriageRequestDTO> prefillRequests = generateRequests(n);
        List<TriageAssessment> prefillAssessments = new ArrayList<>(n);
        for (TriageRequestDTO req : prefillRequests) {
            TriageAssessment ta = new TriageAssessment();
            ta.setTieBreakerScore(scoringStrategy.calculateScore(req));
            ta.setAssignedCategory(TriageCategory.GREEN);
            ta.setCreatedAt(LocalDateTime.now());
            prefillAssessments.add(ta);
        }

        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            PriorityDispatchQueue queue = new PriorityDispatchQueue();
            for (TriageAssessment ta : prefillAssessments) queue.insert(ta);
            for (TriageRequestDTO req : queries) {
                TriageAssessment ta = new TriageAssessment();
                ta.setTieBreakerScore(scoringStrategy.calculateScore(req));
                ta.setAssignedCategory(TriageCategory.GREEN);
                queue.insert(ta);
            }
        }

        double[] times = new double[TIMED_RUNS];
        for (int i = 0; i < TIMED_RUNS; i++) {
            PriorityDispatchQueue queue = new PriorityDispatchQueue();
            for (TriageAssessment ta : prefillAssessments) queue.insert(ta);
            
            long start = System.nanoTime();
            for (TriageRequestDTO req : queries) {
                TriageAssessment ta = new TriageAssessment();
                ta.setTieBreakerScore(scoringStrategy.calculateScore(req));
                ta.setAssignedCategory(TriageCategory.GREEN);
                queue.insert(ta);
            }
            times[i] = (System.nanoTime() - start) / 1_000_000.0;
        }

        double memKb = measureMemory(() -> {
            PriorityDispatchQueue queue = new PriorityDispatchQueue();
            for (TriageAssessment ta : prefillAssessments) queue.insert(ta);
            for (TriageRequestDTO req : queries) {
                TriageAssessment ta = new TriageAssessment();
                ta.setTieBreakerScore(scoringStrategy.calculateScore(req));
                ta.setAssignedCategory(TriageCategory.GREEN);
                queue.insert(ta);
            }
            return queue;
        });

        return logAndCreateResult("WeightedScore+Heap", n, times, memKb, "N/A", writer);
    }

    private BenchmarkResult benchmarkKNN(List<TriageRequestDTO> queries, int n, PrintWriter writer) {
        // Measure resident dataset footprint for KNNClassifier
        // MTS and Heap don't hold an O(N) historical structure in the same way, so this is only for KNN.
        knnClassifier.initSyntheticDataset(0); // clear it first
        forceGC();
        long baseline = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        knnClassifier.initSyntheticDataset(n);
        
        forceGC();
        long afterDataset = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        double baselineKb = baseline / 1024.0;
        double afterDatasetKb = afterDataset / 1024.0;
        double residentFootprintKb = Math.max(0, afterDatasetKb - baselineKb);
        
        System.out.printf("KNN Resident Memory (N=%d): Baseline=%.2f KB, AfterDataset=%.2f KB%n", n, baselineKb, afterDatasetKb);
        
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            for (TriageRequestDTO req : queries) knnClassifier.classify(req);
        }

        double[] times = new double[TIMED_RUNS];
        for (int i = 0; i < TIMED_RUNS; i++) {
            long start = System.nanoTime();
            for (TriageRequestDTO req : queries) knnClassifier.classify(req);
            times[i] = (System.nanoTime() - start) / 1_000_000.0;
        }

        double memKb = measureMemory(() -> {
            List<TriageCategory> res = new ArrayList<>(QUERIES);
            for (TriageRequestDTO req : queries) res.add(knnClassifier.classify(req));
            return res;
        });

        return logAndCreateResult("KNNClassifier", n, times, memKb, String.format("%.2f", residentFootprintKb), writer);
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

    private BenchmarkResult logAndCreateResult(String algo, int n, double[] times, double memKb, String residentKb, PrintWriter writer) {
        Arrays.sort(times);
        double median = (times[times.length / 2] + times[(times.length - 1) / 2]) / 2.0;
        
        double sum = 0;
        for (double t : times) sum += t;
        double avg = sum / times.length;
        
        double sqSum = 0;
        for (double t : times) sqSum += (t - avg) * (t - avg);
        double stddev = Math.sqrt(sqSum / times.length);
        
        writer.printf("%s,%d,%.3f,%.3f,%.3f,%.2f,%s%n", algo, n, avg, median, stddev, memKb, residentKb);
        return new BenchmarkResult(algo, n, avg, median, stddev, memKb, residentKb);
    }

    private List<TriageRequestDTO> generateRequests(int count) {
        List<TriageRequestDTO> list = new ArrayList<>(count);
        Random random = new Random(123);
        for (int i = 0; i < count; i++) {
            TriageRequestDTO req = new TriageRequestDTO();
            req.setBreathing(random.nextBoolean());
            req.setPulseRate(60 + random.nextInt(60));
            req.setAvpu(ConsciousnessLevel.values()[random.nextInt(4)]);
            req.setOxygenSaturation(85 + random.nextInt(16));
            req.setSystolicBP(90 + random.nextInt(50));
            req.setPainScore(random.nextInt(11));
            req.setTemperature(36.0 + random.nextDouble() * 3.0);
            req.setAge(random.nextInt(90));
            req.setHazardPresent(random.nextBoolean());
            list.add(req);
        }
        return list;
    }
    
    private static class BenchmarkResult {
        String algo;
        int n;
        double avgTimeMs;
        double medianTimeMs;
        double stddevTimeMs;
        double memKb;
        String residentKb;

        public BenchmarkResult(String algo, int n, double avgTimeMs, double medianTimeMs, double stddevTimeMs, double memKb, String residentKb) {
            this.algo = algo;
            this.n = n;
            this.avgTimeMs = avgTimeMs;
            this.medianTimeMs = medianTimeMs;
            this.stddevTimeMs = stddevTimeMs;
            this.memKb = memKb;
            this.residentKb = residentKb;
        }
    }
}
