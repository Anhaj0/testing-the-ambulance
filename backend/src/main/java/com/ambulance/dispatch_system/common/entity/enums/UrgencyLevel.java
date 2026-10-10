package com.ambulance.dispatch_system.common.entity.enums;

/**
 * Patient urgency classification used by the Decision (Triage) module to
 * rank concurrent emergency calls. The numeric weight lets the triage
 * scoring function combine urgency with other criteria (e.g. wait time)
 * into a single priority score.
 */
public enum UrgencyLevel {
    CRITICAL(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int weight;

    UrgencyLevel(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
