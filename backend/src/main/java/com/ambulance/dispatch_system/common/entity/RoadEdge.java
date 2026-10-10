package com.ambulance.dispatch_system.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * A directed, weighted edge between two RoadNodes. distanceKm and
 * travelTimeMinutes are alternative edge weights depending on what the
 * routing algorithm optimizes for; "blocked" lets the Route
 * Optimization module exclude closed roads without deleting the edge.
 */
@Entity
public class RoadEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private RoadNode fromNode;

    @ManyToOne(optional = false)
    private RoadNode toNode;

    private double distanceKm;
    private double travelTimeMinutes;
    private boolean blocked = false;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RoadNode getFromNode() { return fromNode; }
    public void setFromNode(RoadNode fromNode) { this.fromNode = fromNode; }

    public RoadNode getToNode() { return toNode; }
    public void setToNode(RoadNode toNode) { this.toNode = toNode; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public double getTravelTimeMinutes() { return travelTimeMinutes; }
    public void setTravelTimeMinutes(double travelTimeMinutes) { this.travelTimeMinutes = travelTimeMinutes; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
}
