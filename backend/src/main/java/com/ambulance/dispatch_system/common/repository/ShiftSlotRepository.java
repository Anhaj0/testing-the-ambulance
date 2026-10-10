package com.ambulance.dispatch_system.common.repository;

import com.ambulance.dispatch_system.common.entity.ShiftSlot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftSlotRepository extends JpaRepository<ShiftSlot, Long> {
}
