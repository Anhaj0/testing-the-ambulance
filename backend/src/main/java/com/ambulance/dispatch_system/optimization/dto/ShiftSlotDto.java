package com.ambulance.dispatch_system.optimization.dto;

import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.entity.enums.Certification;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** API view of a ShiftSlot - one recurring coverage requirement in the weekly roster template. */
public record ShiftSlotDto(
        Long id,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Certification requiredCertification,
        @Min(1) int requiredStaffCount
) {
    public static ShiftSlotDto fromEntity(ShiftSlot slot) {
        return new ShiftSlotDto(slot.getId(), slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime(),
                slot.getRequiredCertification(), slot.getRequiredStaffCount());
    }

    public ShiftSlot toEntity() {
        ShiftSlot slot = new ShiftSlot();
        applyTo(slot);
        return slot;
    }

    public void applyTo(ShiftSlot slot) {
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setRequiredCertification(requiredCertification);
        slot.setRequiredStaffCount(requiredStaffCount);
    }
}
