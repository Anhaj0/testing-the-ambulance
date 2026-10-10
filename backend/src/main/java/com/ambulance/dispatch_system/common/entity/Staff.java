package com.ambulance.dispatch_system.common.entity;

import com.ambulance.dispatch_system.common.entity.enums.Certification;
import com.ambulance.dispatch_system.common.entity.enums.StaffRole;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.HashSet;
import java.util.Set;

/**
 * A member of staff (doctor, paramedic or driver) available to be
 * assigned to shifts. This is the building block the Optimization
 * Module's Genetic Algorithm chooses from: a chromosome is a full
 * roster mapping every ShiftSlot to one Staff member.
 */
@Entity
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private StaffRole role;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<Certification> certifications = new HashSet<>();

    /** Contractual maximum hours per week; exceeding this is penalised by the GA fitness function. */
    private int maxWeeklyHours = 40;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public StaffRole getRole() { return role; }
    public void setRole(StaffRole role) { this.role = role; }

    public Set<Certification> getCertifications() { return certifications; }
    public void setCertifications(Set<Certification> certifications) { this.certifications = certifications; }

    public int getMaxWeeklyHours() { return maxWeeklyHours; }
    public void setMaxWeeklyHours(int maxWeeklyHours) { this.maxWeeklyHours = maxWeeklyHours; }
}
