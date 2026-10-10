package com.ambulance.dispatch_system.common.entity;

import com.ambulance.dispatch_system.common.entity.enums.CallStatus;
import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

/**
 * An incoming emergency call. Links a Patient to an optional dispatched
 * Ambulance and destination Hospital. When several Calls are queued
 * concurrently, the Decision (Triage) module ranks them by the
 * patient's urgencyLevel (plus other criteria) to decide dispatch order.
 */
@Entity
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Patient patient;

    /** Name of the RoadNode nearest to the incident; the routing destination for the ambulance leg. */
    private String locationNode;

    private LocalDateTime receivedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private CallStatus status = CallStatus.RECEIVED;

    @ManyToOne
    private Ambulance assignedAmbulance;

    @ManyToOne
    private Hospital destinationHospital;

    @ElementCollection(fetch = FetchType.EAGER)
        @Enumerated(EnumType.STRING)
        private Set<MedicalEquipment> requiredEquipment = new HashSet<>();


        public Set<MedicalEquipment> getRequiredEquipment() { 
            return requiredEquipment; 
        }

        public void setRequiredEquipment(Set<MedicalEquipment> requiredEquipment) { 
            this.requiredEquipment = requiredEquipment; 
        }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getLocationNode() { return locationNode; }
    public void setLocationNode(String locationNode) { this.locationNode = locationNode; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public CallStatus getStatus() { return status; }
    public void setStatus(CallStatus status) { this.status = status; }

    public Ambulance getAssignedAmbulance() { return assignedAmbulance; }
    public void setAssignedAmbulance(Ambulance assignedAmbulance) { this.assignedAmbulance = assignedAmbulance; }

    public Hospital getDestinationHospital() { return destinationHospital; }
    public void setDestinationHospital(Hospital destinationHospital) { this.destinationHospital = destinationHospital; }
}
