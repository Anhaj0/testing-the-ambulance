package com.ambulance.dispatch_system.triage.benchmark;

import com.ambulance.dispatch_system.common.entity.enums.ConsciousnessLevel;
import com.ambulance.dispatch_system.common.entity.enums.TriageCategory;
import com.ambulance.dispatch_system.triage.dto.TriageRequestDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Synthetic k-nearest-neighbours baseline used only by the Task 4 benchmark.
 * It is intentionally kept outside the deployed application's source set.
 */
class KNNClassifier {

    private static final int K = 5;
    private static final int SYNTHETIC_DATASET_SIZE = 500;
    private final List<HistoricalRecord> dataset = new ArrayList<>();

    void initSyntheticDataset() {
        initSyntheticDataset(SYNTHETIC_DATASET_SIZE);
    }

    void initSyntheticDataset(int size) {
        dataset.clear();
        Random random = new Random(42);
        for (int i = 0; i < size; i++) {
            dataset.add(generateRandomRecord(random));
        }
    }

    TriageCategory classify(TriageRequestDTO request) {
        double[] targetFeatures = extractFeatures(request);
        List<Neighbor> neighbors = new ArrayList<>();

        for (HistoricalRecord record : dataset) {
            double distance = euclideanDistance(targetFeatures, record.features);
            neighbors.add(new Neighbor(distance, record.category));
        }

        neighbors.sort(Comparator.comparingDouble(neighbor -> neighbor.distance));

        int[] votes = new int[TriageCategory.values().length];
        for (int i = 0; i < Math.min(K, neighbors.size()); i++) {
            votes[neighbors.get(i).category.ordinal()]++;
        }

        int maxVotes = -1;
        TriageCategory predictedCategory = TriageCategory.BLUE;
        for (TriageCategory category : TriageCategory.values()) {
            if (votes[category.ordinal()] > maxVotes) {
                maxVotes = votes[category.ordinal()];
                predictedCategory = category;
            }
        }
        return predictedCategory;
    }

    private double[] extractFeatures(TriageRequestDTO request) {
        return new double[]{
                Boolean.TRUE.equals(request.getBreathing()) ? 1.0 : 0.0,
                request.getPulseRate() / 300.0,
                request.getAvpu().ordinal() / 3.0,
                request.getOxygenSaturation() / 100.0,
                request.getSystolicBP() / 300.0,
                request.getPainScore() / 10.0,
                (request.getTemperature() - 20) / 25.0,
                request.getAge() / 130.0,
                Boolean.TRUE.equals(request.getHazardPresent()) ? 1.0 : 0.0
        };
    }

    private double euclideanDistance(double[] first, double[] second) {
        double squaredDistance = 0.0;
        for (int i = 0; i < first.length; i++) {
            double difference = first[i] - second[i];
            squaredDistance += difference * difference;
        }
        return Math.sqrt(squaredDistance);
    }

    private HistoricalRecord generateRandomRecord(Random random) {
        TriageRequestDTO request = new TriageRequestDTO();
        request.setBreathing(random.nextDouble() > 0.05);
        request.setPulseRate(50 + random.nextInt(100));
        request.setAvpu(ConsciousnessLevel.values()[random.nextInt(4)]);
        request.setOxygenSaturation(80 + random.nextInt(21));
        request.setSystolicBP(80 + random.nextInt(80));
        request.setPainScore(random.nextInt(11));
        request.setTemperature(35.0 + random.nextDouble() * 5.0);
        request.setAge(random.nextInt(100));
        request.setHazardPresent(random.nextDouble() > 0.9);

        TriageCategory category = TriageCategory.values()[
                random.nextInt(TriageCategory.values().length)
        ];
        return new HistoricalRecord(extractFeatures(request), category);
    }

    private record HistoricalRecord(double[] features, TriageCategory category) {
    }

    private record Neighbor(double distance, TriageCategory category) {
    }
}
