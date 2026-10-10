package com.ambulance.dispatch_system.common.repository;

import com.ambulance.dispatch_system.common.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    /** Fetches the full roster for a given scheduling week, e.g. for display or re-evaluation. */
    List<Shift> findByWeekStarting(LocalDate weekStarting);
}
