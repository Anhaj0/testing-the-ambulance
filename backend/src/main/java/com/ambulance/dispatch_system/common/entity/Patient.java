package com.ambulance.dispatch_system.common.entity;

import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;
import com.ambulance.dispatch_system.common.entity.enums.UrgencyLevel;
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
 * A patient reported on an emergency Call. urgencyLevel feeds the
 * Decision (Triage) module's ranking; requiredEquipment feeds the
 * Resource Allocation module's ambulance-matching logic.
 */
@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int age;

    /** Free-text description of the condition/symptoms reported by the caller. */
    private String condition;

    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgencyLevel;

    /** Equipment the responding ambulance must carry to treat this patient. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<MedicalEquipment> requiredEquipment = new HashSet<>();

    private String contactNumber;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public UrgencyLevel getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(UrgencyLevel urgencyLevel) { this.urgencyLevel = urgencyLevel; }

    public Set<MedicalEquipment> getRequiredEquipment() { return requiredEquipment; }
    public void setRequiredEquipment(Set<MedicalEquipment> requiredEquipment) { this.requiredEquipment = requiredEquipment; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
}
