package com.ambulance.dispatch_system.common.entity.enums;

/**
 * Medical certifications a staff member may hold. Used by the Shift
 * Scheduling GA's fitness function to penalise shifts left without the
 * skill coverage a ShiftSlot requires (e.g. no ECG-certified staff).
 */
public enum Certification {
    BASIC_LIFE_SUPPORT,
    ADVANCED_LIFE_SUPPORT,
    ECG_CERTIFIED,
    ICU_TRAINED
}
