package com.ambulance.dispatch_system.common.repository;

import com.ambulance.dispatch_system.common.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {
}
