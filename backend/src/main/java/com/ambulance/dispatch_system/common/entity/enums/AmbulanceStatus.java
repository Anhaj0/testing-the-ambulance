package com.ambulance.dispatch_system.common.entity.enums;

/**
 * Lifecycle status of an ambulance, tracked so the Resource Allocation
 * module only considers vehicles that are actually free to dispatch.
 */
public enum AmbulanceStatus {
    AVAILABLE,
    DISPATCHED,
    EN_ROUTE_TO_HOSPITAL,
    AT_HOSPITAL,
    OUT_OF_SERVICE
}
