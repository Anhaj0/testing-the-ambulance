package com.ambulance.dispatch_system.triage;

import com.ambulance.dispatch_system.common.entity.enums.ConsciousnessLevel;
import com.ambulance.dispatch_system.common.entity.enums.TriageCategory;
import com.ambulance.dispatch_system.triage.dto.TriageRequestDTO;
import com.ambulance.dispatch_system.triage.service.impl.algorithms.MTSDecisionTree;
import com.ambulance.dispatch_system.triage.service.impl.algorithms.WeightedScoringStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TriageDecisionEngineTest {

    private MTSDecisionTree mtsDecisionTree;
    private WeightedScoringStrategy scoringStrategy;

    @BeforeEach
    void setUp() {
        mtsDecisionTree = new MTSDecisionTree();
        scoringStrategy = new WeightedScoringStrategy();
    }

    private TriageRequestDTO createBaseRequest() {
        return new TriageRequestDTO(
                true, 80, ConsciousnessLevel.ALERT, 98, 120, 0, 36.5, 30, false, new ArrayList<>()
        );
    }

    @Test
    void testNotBreathingAlwaysRed() {
        TriageRequestDTO req = createBaseRequest();
        req.setBreathing(false); // Immediate red trigger
        req.setOxygenSaturation(99); // Normal SpO2 shouldn't override
        assertEquals(TriageCategory.RED, mtsDecisionTree.evaluate(req));
    }

    @Test
    void testPulseZeroAlwaysRed() {
        TriageRequestDTO req = createBaseRequest();
        req.setPulseRate(0); // Immediate red trigger
        assertEquals(TriageCategory.RED, mtsDecisionTree.evaluate(req));
    }

    @Test
    void testUnresponsiveAlwaysRed() {
        TriageRequestDTO req = createBaseRequest();
        req.setAvpu(ConsciousnessLevel.UNRESPONSIVE); // Immediate red trigger
        assertEquals(TriageCategory.RED, mtsDecisionTree.evaluate(req));
    }

    @Test
    void testSpO2Boundaries() {
        TriageRequestDTO req = createBaseRequest();
        
        req.setOxygenSaturation(89);
        assertEquals(TriageCategory.RED, mtsDecisionTree.evaluate(req));
        
        req.setOxygenSaturation(90);
        assertEquals(TriageCategory.ORANGE, mtsDecisionTree.evaluate(req));

        req.setOxygenSaturation(94);
        assertEquals(TriageCategory.ORANGE, mtsDecisionTree.evaluate(req));
        
        req.setOxygenSaturation(95);
        assertEquals(TriageCategory.YELLOW, mtsDecisionTree.evaluate(req));
        
        req.setOxygenSaturation(97);
        assertEquals(TriageCategory.YELLOW, mtsDecisionTree.evaluate(req));
        
        req.setOxygenSaturation(98);
        assertEquals(TriageCategory.BLUE, mtsDecisionTree.evaluate(req));
    }
    
    @Test
    void testPainScoreBoundaries() {
        TriageRequestDTO req = createBaseRequest();
        
        req.setPainScore(9);
        assertEquals(TriageCategory.ORANGE, mtsDecisionTree.evaluate(req));
        
        req.setPainScore(5);
        assertEquals(TriageCategory.YELLOW, mtsDecisionTree.evaluate(req));
        
        req.setPainScore(8);
        assertEquals(TriageCategory.YELLOW, mtsDecisionTree.evaluate(req));
        
        req.setPainScore(4);
        assertEquals(TriageCategory.GREEN, mtsDecisionTree.evaluate(req));
        
        req.setPainScore(1);
        assertEquals(TriageCategory.GREEN, mtsDecisionTree.evaluate(req));
        
        req.setPainScore(0);
        assertEquals(TriageCategory.BLUE, mtsDecisionTree.evaluate(req));
    }

    @Test
    void testAgeRiskWeighting() {
        TriageRequestDTO base = createBaseRequest();
        
        TriageRequestDTO elderly = createBaseRequest();
        elderly.setAge(66);
        
        TriageRequestDTO pediatric = createBaseRequest();
        pediatric.setAge(4);
        
        double baseScore = scoringStrategy.calculateScore(base);
        double elderlyScore = scoringStrategy.calculateScore(elderly);
        double pediatricScore = scoringStrategy.calculateScore(pediatric);
        
        assertEquals(baseScore + 5.0, elderlyScore, 0.001);
        assertEquals(baseScore + 5.0, pediatricScore, 0.001);
    }
    
    @Test
    void testHazardPresentWeighting() {
        TriageRequestDTO base = createBaseRequest();
        
        TriageRequestDTO hazard = createBaseRequest();
        hazard.setHazardPresent(true);
        
        double baseScore = scoringStrategy.calculateScore(base);
        double hazardScore = scoringStrategy.calculateScore(hazard);
        
        assertEquals(baseScore + 10.0, hazardScore, 0.001);
    }
    
    @Test
    void testEmptySymptomsHandledSafely() {
        TriageRequestDTO req = createBaseRequest();
        req.setSymptoms(null);
        
        // Should not throw NPE
        TriageCategory category = mtsDecisionTree.evaluate(req);
        double score = scoringStrategy.calculateScore(req);
        
        assertEquals(TriageCategory.BLUE, category);
        assertEquals(4.0, score, 0.001); // 100-98 = 2 spo2 deficit * 2.0 = 4.0
    }
}
