package com.ambulance.dispatch_system.common.entity;

import com.ambulance.dispatch_system.common.entity.enums.AmbulanceStatus;
import com.ambulance.dispatch_system.common.entity.enums.MedicalEquipment;
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
 * A single ambulance vehicle in the fleet. Used by the Resource
 * Allocation module (matching equipment/status to a patient's needs) and
 * by the Route Optimization module (currentLocationNode is the starting
 * vertex for routing calculations on the road network graph).
 */
@Entity
public class Ambulance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Fleet registration number, e.g. "AMB-014". */
    private String vehicleNumber;

    /** Name of the RoadNode nearest to the ambulance's current position. */
    private String currentLocationNode;

    @Enumerated(EnumType.STRING)
    private AmbulanceStatus status = AmbulanceStatus.AVAILABLE;

    /** Medical equipment fitted on this vehicle. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private Set<MedicalEquipment> equipment = new HashSet<>();

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public String getCurrentLocationNode() { return currentLocationNode; }
    public void setCurrentLocationNode(String currentLocationNode) { this.currentLocationNode = currentLocationNode; }

    public AmbulanceStatus getStatus() { return status; }
    public void setStatus(AmbulanceStatus status) { this.status = status; }

    public Set<MedicalEquipment> getEquipment() { return equipment; }
    public void setEquipment(Set<MedicalEquipment> equipment) { this.equipment = equipment; }
}
