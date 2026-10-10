package com.ambulance.dispatch_system.resource_allocation.dto;

/**
 * Outcome of a dispatch attempt.
 *
 * @param dispatched whether an ambulance was found and assigned
 * @param callId the emergency call the attempt was made for
 * @param ambulanceVehicleNumber the assigned vehicle's number, or null if none was available
 * @param message human-readable summary for the dispatcher UI
 */
public record DispatchResultDto(
        boolean dispatched,
        Long callId,
        String ambulanceVehicleNumber,
        String message
) {
}
