package com.ambulance.dispatch_system.resource_allocation.dto;

import com.ambulance.dispatch_system.common.entity.Ambulance;
import com.ambulance.dispatch_system.common.entity.enums.AmbulanceStatus;
import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;

import java.util.Set;

/** API view of an Ambulance for the dispatch board. */
public record AmbulanceDto(
        Long id,
        String vehicleNumber,
        String currentLocationNode,
        AmbulanceStatus status,
        Set<MedicalEquipment> equipment
) {
    public static AmbulanceDto fromEntity(Ambulance ambulance) {
        return new AmbulanceDto(
                ambulance.getId(),
                ambulance.getVehicleNumber(),
                ambulance.getCurrentLocationNode(),
                ambulance.getStatus(),
                ambulance.getEquipment()
        );
    }
}
