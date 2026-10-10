package com.ambulance.dispatch_system.resource_allocation.dto;

import com.ambulance.dispatch_system.resource_allocation.optimization.GreedyScheduler;

/**
 * One ambulance's ranking for a specific call, as actually computed by
 * {@link GreedyScheduler} - the real shortest-path travel time and equipment
 * penalty behind its score, not a client-side guess. A list of these, best-first,
 * is what {@code GreedyScheduler.findBestAmbulance} would commit to if asked to dispatch.
 */
public record CandidateDto(
        Long ambulanceId,
        String vehicleNumber,
        double travelMinutes,
        int extraEquipmentCount,
        double score
) {
    public static CandidateDto fromScored(GreedyScheduler.ScoredAmbulance scored) {
        return new CandidateDto(
                scored.ambulance().getId(),
                scored.ambulance().getVehicleNumber(),
                scored.fitness().travelMinutes(),
                scored.fitness().extraEquipmentCount(),
                scored.fitness().score()
        );
    }
}
