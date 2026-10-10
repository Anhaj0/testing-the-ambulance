package com.ambulance.dispatch_system.common.repository;

import com.ambulance.dispatch_system.common.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
}