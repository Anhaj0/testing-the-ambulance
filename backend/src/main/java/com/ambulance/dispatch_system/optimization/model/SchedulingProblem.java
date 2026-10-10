package com.ambulance.dispatch_system.optimization.model;

import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import com.ambulance.dispatch_system.common.entity.Staff;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable definition of one shift-scheduling problem instance: the
 * slots that need covering for a given week, and the pool of staff who
 * may be assigned to them. Both the Genetic Algorithm and the Greedy
 * baseline are scored against the same SchedulingProblem so their
 * results are directly comparable.
 *
 * <p>Each ShiftSlot that requires N staff appears N times in
 * {@code expandedSlots} - one entry per seat that needs filling. A
 * RosterChromosome's genes array is aligned index-for-index with this
 * list: {@code genes[i]} is the Staff member filling
 * {@code expandedSlots.get(i)}.
 */
public record SchedulingProblem(
        List<ShiftSlot> expandedSlots,
        List<Staff> staffPool,
        LocalDate weekStarting
) {

    /** Expands a raw list of ShiftSlots (each wanting requiredStaffCount staff) into one-seat-per-entry form. */
    public static List<ShiftSlot> expand(List<ShiftSlot> shiftSlots) {
        List<ShiftSlot> expanded = new ArrayList<>();
        for (ShiftSlot slot : shiftSlots) {
            for (int i = 0; i < slot.getRequiredStaffCount(); i++) {
                expanded.add(slot);
            }
        }
        return expanded;
    }

    /** Wall-clock start of a ShiftSlot within this problem's scheduling week. */
    public LocalDateTime startOf(ShiftSlot slot) {
        return dateFor(slot.getDayOfWeek()).atTime(slot.getStartTime());
    }

    /** Wall-clock end of a ShiftSlot; handles shifts that cross midnight (end time not after start time). */
    public LocalDateTime endOf(ShiftSlot slot) {
        LocalDateTime end = dateFor(slot.getDayOfWeek()).atTime(slot.getEndTime());
        if (!slot.getEndTime().isAfter(slot.getStartTime())) {
            end = end.plusDays(1);
        }
        return end;
    }

    private LocalDate dateFor(DayOfWeek dayOfWeek) {
        int offset = dayOfWeek.getValue() - weekStarting.getDayOfWeek().getValue();
        if (offset < 0) {
            offset += 7;
        }
        return weekStarting.plusDays(offset);
    }
}
