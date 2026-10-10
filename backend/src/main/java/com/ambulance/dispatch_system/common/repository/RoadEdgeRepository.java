package com.ambulance.dispatch_system.common.repository;

import com.ambulance.dispatch_system.common.entity.RoadEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadEdgeRepository extends JpaRepository<RoadEdge, Long> {

    /** Used to build the in-memory adjacency list for the Route Optimization module. */
    List<RoadEdge> findByBlockedFalse();
}
