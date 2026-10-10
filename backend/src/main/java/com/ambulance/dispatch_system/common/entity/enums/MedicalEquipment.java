package com.ambulance.dispatch_system.common.entity.enums;

/**
 * Equipment an ambulance may carry. The Resource Allocation module
 * matches a Patient's requiredEquipment against an Ambulance's equipment
 * set when choosing which vehicle to dispatch.
 */
public enum MedicalEquipment {
    DEFIBRILLATOR,
    VENTILATOR,
    ECG_MONITOR,
    OXYGEN_SUPPLY,
    ICU_EQUIPMENT
}
