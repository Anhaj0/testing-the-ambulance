package com.ambulance.dispatch_system.triage.service.impl.algorithms;

import com.ambulance.dispatch_system.common.entity.enums.ConsciousnessLevel;
import com.ambulance.dispatch_system.common.entity.enums.TriageCategory;
import com.ambulance.dispatch_system.triage.dto.TriageRequestDTO;
import org.springframework.stereotype.Component;

/**
 * Manchester Triage System (MTS) Decision Tree implementation.
 * 
 * Time Complexity: O(1)
 * Justification: The decision tree evaluates a fixed number of physiological parameters
 * using standard if-else conditional branches. Execution time is constant regardless of 
 * historical data or queue size.
 */
@Component
public class MTSDecisionTree {

    // Threshold Constants
    private static final int RED_SPO2_THRESHOLD = 90;
    private static final int RED_SYSTOLIC_BP_THRESHOLD = 90;
    
    private static final int ORANGE_SPO2_LOWER = 90;
    private static final int ORANGE_SPO2_UPPER = 94;
    private static final int ORANGE_PULSE_UPPER = 120;
    private static final int ORANGE_PULSE_LOWER = 50;
    private static final double ORANGE_TEMP_UPPER = 40.0;
    private static final double ORANGE_TEMP_LOWER = 35.0;
    private static final int ORANGE_PAIN_LOWER = 9;
    
    private static final int YELLOW_SPO2_LOWER = 95;
    private static final int YELLOW_SPO2_UPPER = 97;
    private static final int YELLOW_PAIN_LOWER = 5;
    private static final int YELLOW_PAIN_UPPER = 8;
    
    private static final int GREEN_PAIN_LOWER = 1;
    private static final int GREEN_PAIN_UPPER = 4;

    public TriageCategory evaluate(TriageRequestDTO request) {
        // RED (Immediate)
        if (!request.getBreathing() || 
            request.getPulseRate() == 0 || 
            request.getAvpu() == ConsciousnessLevel.UNRESPONSIVE || 
            request.getOxygenSaturation() < RED_SPO2_THRESHOLD || 
            request.getSystolicBP() < RED_SYSTOLIC_BP_THRESHOLD) {
            return TriageCategory.RED;
        }

        // ORANGE (Very Urgent)
        if ((request.getOxygenSaturation() >= ORANGE_SPO2_LOWER && request.getOxygenSaturation() <= ORANGE_SPO2_UPPER) ||
            request.getAvpu() == ConsciousnessLevel.PAIN ||
            request.getPulseRate() > ORANGE_PULSE_UPPER || 
            request.getPulseRate() < ORANGE_PULSE_LOWER ||
            request.getTemperature() > ORANGE_TEMP_UPPER || 
            request.getTemperature() < ORANGE_TEMP_LOWER ||
            request.getPainScore() >= ORANGE_PAIN_LOWER) {
            return TriageCategory.ORANGE;
        }

        // YELLOW (Urgent)
        if ((request.getOxygenSaturation() >= YELLOW_SPO2_LOWER && request.getOxygenSaturation() <= YELLOW_SPO2_UPPER) ||
            request.getAvpu() == ConsciousnessLevel.VOICE ||
            (request.getPainScore() >= YELLOW_PAIN_LOWER && request.getPainScore() <= YELLOW_PAIN_UPPER)) {
            return TriageCategory.YELLOW;
        }

        // GREEN (Standard)
        if (request.getPainScore() >= GREEN_PAIN_LOWER && request.getPainScore() <= GREEN_PAIN_UPPER) {
            return TriageCategory.GREEN;
        }

        // BLUE (Non-Urgent)
        return TriageCategory.BLUE;
    }
}
