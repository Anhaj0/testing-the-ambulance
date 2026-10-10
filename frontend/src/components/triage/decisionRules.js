// Presentation-only mirror of the backend's triage algorithms, so the UI can
// explain *why* a category and score came back instead of just displaying
// the numbers. The backend (MTSDecisionTree.java / WeightedScoringStrategy.java,
// both in triage/service/impl/algorithms) remains the sole source of truth —
// this never decides anything, it only re-derives, from the same submitted
// vitals, which of the backend's named threshold checks explain the result
// already returned. Same convention this codebase already uses for backend
// enums mirrored client-side (e.g. StaffTab's CERTIFICATIONS list) — if the
// thresholds below ever drift from MTSDecisionTree.java, this explanation
// (not the actual triage outcome) is what goes stale.

// --- MTSDecisionTree.java thresholds, verbatim ---
const RED_SPO2_THRESHOLD = 90;
const RED_SYSTOLIC_BP_THRESHOLD = 90;

const ORANGE_SPO2_LOWER = 90;
const ORANGE_SPO2_UPPER = 94;
const ORANGE_PULSE_UPPER = 120;
const ORANGE_PULSE_LOWER = 50;
const ORANGE_TEMP_UPPER = 40.0;
const ORANGE_TEMP_LOWER = 35.0;
const ORANGE_PAIN_LOWER = 9;

const YELLOW_SPO2_LOWER = 95;
const YELLOW_SPO2_UPPER = 97;
const YELLOW_PAIN_LOWER = 5;
const YELLOW_PAIN_UPPER = 8;

const GREEN_PAIN_LOWER = 1;
const GREEN_PAIN_UPPER = 4;

/**
 * Re-runs the same RED -> ORANGE -> YELLOW -> GREEN -> BLUE short-circuit
 * MTSDecisionTree.evaluate() does, but instead of stopping at the first
 * matching category, collects every rule within *that* category's OR-chain
 * that was true — so a patient who is both hypoxic and in severe pain sees
 * both reasons, not just whichever the tree checked first.
 */
export function explainCategory(assessment) {
  const {
    breathing, pulseRate, avpu, oxygenSaturation, systolicBP, painScore, temperature,
  } = assessment;

  const redReasons = [];
  if (!breathing) redReasons.push('Not breathing');
  if (pulseRate === 0) redReasons.push('No pulse detected');
  if (avpu === 'UNRESPONSIVE') redReasons.push('Unresponsive (AVPU)');
  if (oxygenSaturation < RED_SPO2_THRESHOLD) {
    redReasons.push(`SpO₂ ${oxygenSaturation}% is below the ${RED_SPO2_THRESHOLD}% RED threshold`);
  }
  if (systolicBP < RED_SYSTOLIC_BP_THRESHOLD) {
    redReasons.push(`Systolic BP ${systolicBP} mmHg is below the ${RED_SYSTOLIC_BP_THRESHOLD} mmHg RED threshold`);
  }
  if (redReasons.length) return { category: 'RED', reasons: redReasons };

  const orangeReasons = [];
  if (oxygenSaturation >= ORANGE_SPO2_LOWER && oxygenSaturation <= ORANGE_SPO2_UPPER) {
    orangeReasons.push(`SpO₂ ${oxygenSaturation}% is in the ${ORANGE_SPO2_LOWER}–${ORANGE_SPO2_UPPER}% ORANGE range`);
  }
  if (avpu === 'PAIN') orangeReasons.push('Responds only to pain (AVPU)');
  if (pulseRate > ORANGE_PULSE_UPPER) {
    orangeReasons.push(`Pulse ${pulseRate} bpm is above the ${ORANGE_PULSE_UPPER} bpm ORANGE threshold`);
  }
  if (pulseRate < ORANGE_PULSE_LOWER) {
    orangeReasons.push(`Pulse ${pulseRate} bpm is below the ${ORANGE_PULSE_LOWER} bpm ORANGE threshold`);
  }
  if (temperature > ORANGE_TEMP_UPPER) {
    orangeReasons.push(`Temperature ${temperature}°C is above the ${ORANGE_TEMP_UPPER}°C ORANGE threshold`);
  }
  if (temperature < ORANGE_TEMP_LOWER) {
    orangeReasons.push(`Temperature ${temperature}°C is below the ${ORANGE_TEMP_LOWER}°C ORANGE threshold`);
  }
  if (painScore >= ORANGE_PAIN_LOWER) {
    orangeReasons.push(`Pain score ${painScore}/10 meets the ORANGE threshold (≥${ORANGE_PAIN_LOWER})`);
  }
  if (orangeReasons.length) return { category: 'ORANGE', reasons: orangeReasons };

  const yellowReasons = [];
  if (oxygenSaturation >= YELLOW_SPO2_LOWER && oxygenSaturation <= YELLOW_SPO2_UPPER) {
    yellowReasons.push(`SpO₂ ${oxygenSaturation}% is in the ${YELLOW_SPO2_LOWER}–${YELLOW_SPO2_UPPER}% YELLOW range`);
  }
  if (avpu === 'VOICE') yellowReasons.push('Responds to voice only (AVPU)');
  if (painScore >= YELLOW_PAIN_LOWER && painScore <= YELLOW_PAIN_UPPER) {
    yellowReasons.push(`Pain score ${painScore}/10 is in the YELLOW range (${YELLOW_PAIN_LOWER}–${YELLOW_PAIN_UPPER})`);
  }
  if (yellowReasons.length) return { category: 'YELLOW', reasons: yellowReasons };

  if (painScore >= GREEN_PAIN_LOWER && painScore <= GREEN_PAIN_UPPER) {
    return {
      category: 'GREEN',
      reasons: [`Pain score ${painScore}/10 is in the GREEN range (${GREEN_PAIN_LOWER}–${GREEN_PAIN_UPPER})`],
    };
  }

  return { category: 'BLUE', reasons: ['No RED/ORANGE/YELLOW/GREEN criteria met'] };
}

// --- WeightedScoringStrategy.java weights, verbatim ---
const SPO2_DEFICIT_WEIGHT = 2.0;
const PAIN_SCORE_WEIGHT = 1.5;
const AGE_RISK_BONUS = 5.0;
const HAZARD_BONUS = 10.0;

/**
 * Re-derives WeightedScoringStrategy.calculateScore()'s components (not just
 * the total the backend already returns) so the tie-breaker used within a
 * category is as visible as the category itself.
 */
export function explainScore(assessment) {
  const { oxygenSaturation, painScore, age, hazardPresent } = assessment;
  const breakdown = [];

  const spo2Deficit = Math.max(0, 100 - oxygenSaturation);
  const spo2Contribution = spo2Deficit * SPO2_DEFICIT_WEIGHT;
  breakdown.push({
    label: `SpO₂ deficit: ${spo2Deficit} pts below 100% × ${SPO2_DEFICIT_WEIGHT}`,
    value: spo2Contribution,
  });

  const painContribution = painScore * PAIN_SCORE_WEIGHT;
  breakdown.push({ label: `Pain score: ${painScore} × ${PAIN_SCORE_WEIGHT}`, value: painContribution });

  const ageRisk = age > 65 || age < 5;
  if (ageRisk) {
    breakdown.push({ label: `Age-risk bonus (age ${age} is over 65 or under 5)`, value: AGE_RISK_BONUS });
  }

  if (hazardPresent) {
    breakdown.push({ label: 'Hazard-present bonus', value: HAZARD_BONUS });
  }

  const total = breakdown.reduce((sum, item) => sum + item.value, 0);
  return { total, breakdown };
}
