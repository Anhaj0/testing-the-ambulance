package com.ambulance.dispatch_system.triage.service.impl.algorithms;

import com.ambulance.dispatch_system.triage.dto.TriageRequestDTO;
import org.springframework.stereotype.Component;

/**
 * Calculates a weighted tie-breaker score for patients who belong
 * to the same triage category.
 *
 * The score considers oxygen saturation, pain level, age-related risk,
 * and the presence of a hazard. A higher score indicates a greater
 * need for prioritisation within the same category.
 *
 * Time Complexity: O(1)
 * Justification: The calculation uses a fixed number of physiological
 * parameters and constant-time arithmetic operations.
 */
@Component
public class WeightedScoringStrategy {

    // Weight applied for each percentage point below normal oxygen saturation.
    private static final double SPO2_DEFICIT_WEIGHT = 2.0;

    // Weight applied for each point in the patient's pain score.
    private static final double PAIN_SCORE_WEIGHT = 1.5;

    // Additional score for patients in higher-risk age groups.
    private static final double AGE_RISK_BONUS = 5.0;

    // Additional score when an environmental or safety hazard is present.
    private static final double HAZARD_BONUS = 10.0;

    /**
     * Calculates the weighted score for a triage request.
     *
     * @param request patient assessment data used for scoring
     * @return calculated weighted tie-breaker score
     */
    public double calculateScore(TriageRequestDTO request) {
        double score = 0.0;

        // Calculate the oxygen saturation deficit from 100%.
        int oxygenSaturationDeficit =
                Math.max(0, 100 - request.getOxygenSaturation());

        score += oxygenSaturationDeficit * SPO2_DEFICIT_WEIGHT;

        // Add the patient's pain score contribution.
        score += request.getPainScore() * PAIN_SCORE_WEIGHT;

        // Apply an additional score for higher-risk age groups.
        if (request.getAge() > 65 || request.getAge() < 5) {
            score += AGE_RISK_BONUS;
        }

        // Add a bonus when a hazard is present.
        if (Boolean.TRUE.equals(request.getHazardPresent())) {
            score += HAZARD_BONUS;
        }

        return score;
    }
}