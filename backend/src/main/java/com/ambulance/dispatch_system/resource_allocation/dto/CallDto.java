package com.ambulance.dispatch_system.resource_allocation.dto;

import com.ambulance.dispatch_system.common.entity.Call;
import com.ambulance.dispatch_system.common.entity.enums.CallStatus;
import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;
import com.ambulance.dispatch_system.common.entity.enums.UrgencyLevel;

import java.time.LocalDateTime;
import java.util.Set;

/** API view of an emergency Call for the dispatch board - just what a dispatcher needs, without exposing the JPA entity graph (Patient, Ambulance, Hospital). */
public record CallDto(
        Long id,
        String patientName,
        UrgencyLevel urgencyLevel,
        String locationNode,
        LocalDateTime receivedAt,
        CallStatus status,
        Set<MedicalEquipment> requiredEquipment,
        String assignedAmbulanceVehicleNumber,
        Long patientId,
        Integer patientAge
) {
    public static CallDto fromEntity(Call call) {
        return new CallDto(
                call.getId(),
                call.getPatient() != null ? call.getPatient().getName() : null,
                call.getPatient() != null ? call.getPatient().getUrgencyLevel() : null,
                call.getLocationNode(),
                call.getReceivedAt(),
                call.getStatus(),
                call.getRequiredEquipment(),
                call.getAssignedAmbulance() != null ? call.getAssignedAmbulance().getVehicleNumber() : null,
                call.getPatient() != null ? call.getPatient().getId() : null,
                call.getPatient() != null ? call.getPatient().getAge() : null
        );
    }
}
