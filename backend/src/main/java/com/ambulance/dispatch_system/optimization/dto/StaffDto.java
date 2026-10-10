package com.ambulance.dispatch_system.optimization.dto;

import com.ambulance.dispatch_system.common.entity.Staff;
import com.ambulance.dispatch_system.common.entity.enums.Certification;
import com.ambulance.dispatch_system.common.entity.enums.StaffRole;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/** API view of a Staff member - both the request body for create/update and the response shape. */
public record StaffDto(
        Long id,
        @NotBlank String name,
        @NotNull StaffRole role,
        Set<Certification> certifications,
        @Min(1) int maxWeeklyHours
) {
    public static StaffDto fromEntity(Staff staff) {
        return new StaffDto(staff.getId(), staff.getName(), staff.getRole(), staff.getCertifications(), staff.getMaxWeeklyHours());
    }

    public Staff toEntity() {
        Staff staff = new Staff();
        applyTo(staff);
        return staff;
    }

    public void applyTo(Staff staff) {
        staff.setName(name);
        staff.setRole(role);
        staff.setCertifications(certifications != null ? certifications : Set.of());
        staff.setMaxWeeklyHours(maxWeeklyHours);
    }
}
