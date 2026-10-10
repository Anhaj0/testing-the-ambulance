package com.ambulance.dispatch_system.optimization.dto;

import com.ambulance.dispatch_system.common.entity.Shift;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/** Read-only view of one Staff-to-shift assignment, whether persisted or just previewed. */
public record ShiftDto(
        Long id,
        Long staffId,
        String staffName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate weekStarting
) {
    public static ShiftDto fromEntity(Shift shift) {
        return new ShiftDto(
                shift.getId(),
                shift.getStaff().getId(),
                shift.getStaff().getName(),
                shift.getShiftSlot().getDayOfWeek(),
                shift.getShiftSlot().getStartTime(),
                shift.getShiftSlot().getEndTime(),
                shift.getWeekStarting());
    }
}
