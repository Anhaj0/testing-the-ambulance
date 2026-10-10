package com.ambulance.dispatch_system.common.entity.enums;

/**
 * Manchester Triage System (MTS) Categories
 */
public enum TriageCategory {
    RED(5),     // Immediate
    ORANGE(4),  // Very Urgent
    YELLOW(3),  // Urgent
    GREEN(2),   // Standard
    BLUE(1);    // Non-Urgent

    private final int severity;

    TriageCategory(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }
}
