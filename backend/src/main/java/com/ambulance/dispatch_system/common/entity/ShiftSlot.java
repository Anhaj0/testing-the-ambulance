package com.ambulance.dispatch_system.common.entity;

import com.ambulance.dispatch_system.common.entity.enums.Certification;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

/**
 * A single coverage requirement in the weekly roster template, e.g.
 * "Monday 08:00-16:00, needs 1 ECG-certified staff member". The
 * Optimization Module (GA / Greedy baseline) decides which Staff fills
 * each ShiftSlot; each decision is persisted as a Shift.
 */
@Entity
public class ShiftSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private LocalTime startTime;
    private LocalTime endTime;

    /** Certification required to fill this slot; null if any staff member qualifies. */
    @Enumerated(EnumType.STRING)
    private Certification requiredCertification;

    /** Number of staff members needed to fully cover this slot. */
    private int requiredStaffCount = 1;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Certification getRequiredCertification() { return requiredCertification; }
    public void setRequiredCertification(Certification requiredCertification) { this.requiredCertification = requiredCertification; }

    public int getRequiredStaffCount() { return requiredStaffCount; }
    public void setRequiredStaffCount(int requiredStaffCount) { this.requiredStaffCount = requiredStaffCount; }

    /** Shift duration in hours, used by the GA fitness function to sum a staff member's weekly hours. */
    @Transient
    public double getDurationHours() {
        return Duration.between(startTime, endTime).toMinutes() / 60.0;
    }
}
