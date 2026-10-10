package com.ambulance.dispatch_system.common.entity.enums;

/**
 * Job role of a staff member. Some ShiftSlots may implicitly require a
 * specific role (e.g. a DOCTOR) in addition to any Certification.
 */
public enum StaffRole {
    DOCTOR,
    PARAMEDIC,
    DRIVER
}
