package com.ambulance.dispatch_system.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;

/**
 * A concrete assignment of one Staff member to one ShiftSlot for a
 * specific scheduling week. This is the unit of output produced by the
 * Optimization Module (Genetic Algorithm / Greedy baseline) once a
 * roster has been chosen.
 */
@Entity
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private ShiftSlot shiftSlot;

    @ManyToOne(optional = false)
    private Staff staff;

    /** Monday of the scheduling week this assignment belongs to. */
    private LocalDate weekStarting;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ShiftSlot getShiftSlot() { return shiftSlot; }
    public void setShiftSlot(ShiftSlot shiftSlot) { this.shiftSlot = shiftSlot; }

    public Staff getStaff() { return staff; }
    public void setStaff(Staff staff) { this.staff = staff; }

    public LocalDate getWeekStarting() { return weekStarting; }
    public void setWeekStarting(LocalDate weekStarting) { this.weekStarting = weekStarting; }
}
