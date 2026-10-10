package com.ambulance.dispatch_system.common.repository;

import com.ambulance.dispatch_system.common.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
