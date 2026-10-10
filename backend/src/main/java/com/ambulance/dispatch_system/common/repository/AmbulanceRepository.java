package com.ambulance.dispatch_system.common.repository;

import com.ambulance.dispatch_system.common.entity.Ambulance;
import com.ambulance.dispatch_system.common.entity.enums.AmbulanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {

    /** Used by the Resource Allocation module to shortlist dispatch candidates. */
    List<Ambulance> findByStatus(AmbulanceStatus status);
}
