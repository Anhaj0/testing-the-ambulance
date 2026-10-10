package com.ambulance.dispatch_system.common.entity.enums;

/**
 * Lifecycle status of an emergency Call, from first ringing in to the
 * ambulance completing the job.
 */
public enum CallStatus {
    RECEIVED,
    TRIAGED,
    DISPATCHED,
    EN_ROUTE,
    COMPLETED,
    CANCELLED
}
